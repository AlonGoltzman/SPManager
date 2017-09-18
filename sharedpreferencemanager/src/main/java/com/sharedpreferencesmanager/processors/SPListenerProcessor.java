package com.sharedpreferencesmanager.processors;

import com.sharedpreferencesmanager.annotations.SPUpdateTarget;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import static com.sharedpreferencesmanager.processors.SPManagerProcessor.defaultSPMName;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;


@SupportedAnnotationTypes("com.sharedpreferencesmanager.annotations.SPListener")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SPListenerProcessor extends AbstractProcessor {

    private static final String listenerAnnotationRef = "com.sharedpreferencesmanager.annotations.SPListener";
    private static final String targetAnnotationRef = "com.sharedpreferencesmanager.annotations.SPUpdateTarget";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement spmTypeElement = processingEnv.getElementUtils().getTypeElement(listenerAnnotationRef);
        Set<? extends Element> allAnnotatedElements = roundEnv.getElementsAnnotatedWith(spmTypeElement);
        if (!ensureObjectParent(allAnnotatedElements))
            return false;
        Set<? extends Element> allElementsWithMethods = ensureHasMethods(allAnnotatedElements);
        if (allElementsWithMethods != null) {
            Set<ElementInformationContainer> allElementInformation = ensureMethodInformation(allElementsWithMethods);
            try {
                incorporateInformationIntoSPM(allElementInformation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean ensureObjectParent(Set<? extends Element> elements) {
        print(NOTE, "Ensure direct superclass is Object.");
        for (Element element : elements) {
            for (TypeMirror superType : processingEnv.getTypeUtils().directSupertypes(element.asType())) {
                TypeMirror objectType = processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
                if (!processingEnv.getTypeUtils().isSameType(superType, objectType)) {
                    processingEnv.getMessager().printMessage(ERROR, "All classes annotated with @SPListener must not be the child of any class (i.e. no extends).", element);
                    return false;
                }
            }
        }
        return true;
    }

    private void incorporateInformationIntoSPM(Set<ElementInformationContainer> allElementInformation) throws IOException {
        print(NOTE, "Incorporating information and generating SPManager");
        Element spElement = processingEnv.getElementUtils().getTypeElement("android.content.SharedPreferences");
        JavaFileObject spm = processingEnv.getFiler().createSourceFile(defaultSPMName, spElement);
        BufferedWriter writer = new BufferedWriter(spm.openWriter());
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/spm.template")));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("%$PLACEHOLDER$%")) {
                writer.write(getAllInformation(allElementInformation));
            } else
                writer.write(line);
            writer.newLine();
        }

        writer.flush();
        writer.close();
        reader.close();
    }

    @Nullable
    private Set<? extends Element> ensureHasMethods(Set<? extends Element> allAnnotatedElements) {
        print(NOTE, "Ensuring all annotated elements have proper method.");
        if (allAnnotatedElements.size() == 0)
            return null;
        Set<Element> allElementsWithMethods = new HashSet<>();
        for (Element container : allAnnotatedElements) {
            boolean hasMethod = false;
            for (Element method : container.getEnclosedElements()) {
                if (!method.getKind().equals(ElementKind.METHOD))
                    continue;
                List<? extends AnnotationMirror> annotations = method.getAnnotationMirrors();
                Element annotationElement = processingEnv.getElementUtils().getTypeElement(targetAnnotationRef);
                boolean matchingAnnotation = false;
                for (AnnotationMirror annotation : annotations)
                    if (processingEnv.getTypeUtils().isAssignable(annotation.getAnnotationType(), annotationElement.asType())) {
                        matchingAnnotation = true;
                        break;
                    }
                if (!matchingAnnotation) {
                    print(ERROR,
                            "Any class annotated with @SPListener must contain at least one method annotated with @SPUpdateTarget.");
                    return null;
                }
                allElementsWithMethods.add(container);
                hasMethod = true;
                break;
            }
            if (!hasMethod) {
                processingEnv.getMessager().printMessage(ERROR,
                        "Any class annotated with @SPListener must contain at least one method annotated with @SPUpdateTarget.", container);
                return null;
            }
        }
        return allElementsWithMethods;
    }

    private Set<ElementInformationContainer> ensureMethodInformation(Set<? extends Element> allElementsWithMethods) {
        print(NOTE, "Finding all method information (i.e. class name, method name and keys).");
        Set<ElementInformationContainer> containers = new HashSet<>();
        for (Element elementWithMethod : allElementsWithMethods)
            for (Element enclosedElement : elementWithMethod.getEnclosedElements()) {
                if (!enclosedElement.getKind().equals(ElementKind.METHOD))
                    continue;
                List<? extends AnnotationMirror> allAnnotationsOnEnclosedElement = enclosedElement.getAnnotationMirrors();
                TypeMirror requiredAnnotationType = processingEnv.getElementUtils().getTypeElement(targetAnnotationRef).asType();
                for (AnnotationMirror annotationMirror : allAnnotationsOnEnclosedElement)
                    if (processingEnv.getTypeUtils().isAssignable(annotationMirror.getAnnotationType(), requiredAnnotationType)) {
                        ensureCorrectParameters((ExecutableElement) enclosedElement);
                        SPUpdateTarget updateTarget = enclosedElement.getAnnotation(SPUpdateTarget.class);
                        String[] keys = updateTarget.keys();
                        ElementInformationContainer container =
                                new ElementInformationContainer(elementWithMethod.toString(),
                                        enclosedElement.toString().substring(0, enclosedElement.toString().indexOf("(")),
                                        keys);
                        containers.add(container);
                    }
            }

        return containers;
    }


    private void ensureCorrectParameters(ExecutableElement method) {
        print(NOTE, "Ensuring method has correct parameters");
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != 3)
            processingEnv.getMessager().printMessage(ERROR,
                    "Any method annotated with @SPUpdateTarget, must receive 3 parameters, Context, String & Object, in this order.");
        Element contextType = processingEnv.getElementUtils().getTypeElement("android.content.Context");
        Element stringType = processingEnv.getElementUtils().getTypeElement("java.lang.String");
        Element objectType = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
        Element paramFirst = parameters.get(0);
        Element paramSecond = parameters.get(1);
        Element paramThird = parameters.get(2);
        boolean firstMatch = processingEnv.getTypeUtils().isAssignable(contextType.asType(), paramFirst.asType());
        boolean secondMatch = processingEnv.getTypeUtils().isAssignable(stringType.asType(), paramSecond.asType());
        boolean thirdMatch = processingEnv.getTypeUtils().isAssignable(objectType.asType(), paramThird.asType());
        if (!firstMatch)
            processingEnv.getMessager().printMessage(ERROR,
                    "First parameter in the method is incorrect, needs to be Context.",
                    method);
        else if (!secondMatch)
            processingEnv.getMessager().printMessage(ERROR,
                    "Second parameter in the method is incorrect, needs to be String.",
                    method);
        else if (!thirdMatch)
            processingEnv.getMessager().printMessage(ERROR,
                    "Third parameter in the method is incorrect, needs to be Object.",
                    method);
    }

    private String getAllInformation(Set<ElementInformationContainer> containers) {
        print(NOTE, "Constructing string from element information containers.");
        StringBuilder builder = new StringBuilder();
        for (ElementInformationContainer container : containers)
            builder.append("\t\t").append("\"").append(container.getElementName()).append("\"")
                    .append(",")
                    .append("\"").append(container.getAnnotatedMethodName()).append("\"")
                    .append(",")
                    .append("\"").append(Arrays.toString(container.keysToUpdate)).append("\"")
                    .append(",").append("\n");
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    private void print(Diagnostic.Kind kind, String msg) {
        processingEnv.getMessager().printMessage(kind, msg);
    }
}
