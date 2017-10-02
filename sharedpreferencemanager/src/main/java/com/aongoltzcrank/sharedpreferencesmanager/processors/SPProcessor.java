package com.aongoltzcrank.sharedpreferencesmanager.processors;

import com.aongoltzcrank.sharedpreferencesmanager.annotations.SPUpdateTarget;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager", "com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager"})
public class SPProcessor extends AbstractProcessor {

    private static final String DEFAULT_SPL_FILENAME = "com.sharedpreferencesmanager.generated.SharedPreferencesListeners";
    private static final String DEFAULT_SPM_FILENAME = "com.sharedpreferencesmanager.generated.SharedPreferencesManager";

    private static final String ANNOTATION_REF_SPMANAGER = "com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager";
    private static final String ANNOTATION_REF_SPLISTENER = "com.aongoltzcrank.sharedpreferencesmanager.annotations.SPListener";
    private static final String ANNOTATION_REF_SPTARGET = "com.aongoltzcrank.sharedpreferencesmanager.annotations.SPUpdateTarget";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement typeSPM = getType(ANNOTATION_REF_SPMANAGER);
        TypeElement typeSPL = getType(ANNOTATION_REF_SPLISTENER);

        Set<? extends Element> allSPMElements = roundEnv.getElementsAnnotatedWith(typeSPM);
        printNote("Starting checks....");
        printNote("Starting check on SPM....");
        if (beginSPMCheck(allSPMElements)) {
            printNote("Check on SPM was successful....");
            printNote("Trying to create SPM....");
            try {
                createSPM();
            } catch (IOException e) {
                e.printStackTrace();
                printNote("SPM Creation failed....");
            }
            printNote("SPM Creation worked....");
            beginSPLCheck(roundEnv.getElementsAnnotatedWith(typeSPL));
        }


        return false;
    }

    private boolean beginSPMCheck(Set<? extends Element> allSPMElements) {
        return ensureSingleSPM(allSPMElements) && ensureSingleton(allSPMElements.iterator().next());
    }

    private void beginSPLCheck(Set<? extends Element> allSPLElements) {
        printNote("Starting SPL check....");
        List<ElementInformationContainer> splInformations = new LinkedList<>();
        for (Element splElement : allSPLElements) {
            List<Element> allMethods = new LinkedList<>();
            printNote("Starting SPL method check....");
            if (ensureSPLMethod(allMethods, splElement))
                splInformations.addAll(Arrays.asList(createEIC(allMethods, splElement)));
            else
                return;
            printNote("SPL method check was successful....");
        }
        printNote("Trying to create SPL....");
        try {
            createSPL(splInformations);
        } catch (IOException e) {
            e.printStackTrace();
            printNote("SPL creation failed....");
        }

    }
    //----------------------------------------------------------------------------------------------

    private void createSPM() throws IOException {
        Element spElement = processingEnv.getElementUtils().getTypeElement("android.content.SharedPreferences");
        JavaFileObject spm = processingEnv.getFiler().createSourceFile(DEFAULT_SPM_FILENAME, spElement);
        BufferedWriter writer = new BufferedWriter(spm.openWriter());
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/spmgr.template")));

        String line;
        int placeholderCount = 0;
        while ((line = reader.readLine()) != null) {
            if (line.contains("%$PLACEHOLDER$%"))
                writer.write(getRequiredLine(placeholderCount++));
            else
                writer.write(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
        reader.close();
    }

    private void createSPL(List<ElementInformationContainer> containers) throws IOException {
        JavaFileObject spm = processingEnv.getFiler().createSourceFile(DEFAULT_SPL_FILENAME);
        BufferedWriter writer = new BufferedWriter(spm.openWriter());
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/splisteners.template")));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("%$PLACEHOLDER$%")) {
                writer.write(getContainerInformationString(containers));
            } else
                writer.write(line);
            writer.newLine();
        }
        writer.flush();
        writer.close();
        reader.close();
    }

    private String getContainerInformationString(List<ElementInformationContainer> containers) {
        StringBuilder builder = new StringBuilder();
        for (ElementInformationContainer container : containers)
            builder.append("\t\t").append("\"").append(container.getElementName()).append("\"")
                    .append(",")
                    .append("\"").append(container.getAnnotatedMethodName()).append("\"")
                    .append(",")
                    .append("\"").append(Arrays.toString(container.keysToUpdate)).append("\"")
                    .append(",").append("\n");
        printNote("Output container information: " + builder.toString());
        builder.setLength(builder.length() <= 2 ? builder.length() : builder.length() - 2);
        return builder.toString();
    }

    private String getRequiredLine(int counter) {
        switch (counter) {
            case 0:
                return "\tprivate " + DEFAULT_SPL_FILENAME + " mListeners;";
            case 1:
                return "\t\tmListeners = " + DEFAULT_SPL_FILENAME + ".getInstance();";
            case 2:
                return "\t\tmListeners.updated(context,key,value);";
            default:
                printError("Had too many placeholders in spmgr.template.");
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------

    private boolean ensureSPLMethod(List<Element> allMethods, Element spl) {
        if (spl == null)
            return false;
        for (Element element : spl.getEnclosedElements()) {
            if (!element.getKind().equals(ElementKind.METHOD))
                continue;
            if (!checkAnnotated(element))
                continue;
            if (!ensureSPLMethodParamters(element)) {
                printError("All methods annotated with @SPUpdateTarget must have the following parameters in this order:\n-Context\n-String\n-Object", element);
                return false;
            }
            allMethods.add(element);
        }
        return true;
    }

    private boolean checkAnnotated(Element method) {
        TypeMirror target = getType(ANNOTATION_REF_SPTARGET).asType();
        Types typeUtil = processingEnv.getTypeUtils();
        for (AnnotationMirror annotation : method.getAnnotationMirrors())
            if (typeUtil.isAssignable(annotation.getAnnotationType(), target))
                return true;
        return false;
    }

    private boolean ensureSPLMethodParamters(Element element) {
        ExecutableElement method = (ExecutableElement) element;
        Types typeUtil = processingEnv.getTypeUtils();
        List<? extends VariableElement> params = method.getParameters();
        if (params.size() != 3)
            return false;
        Element contextElem = getType("android.content.Context");
        Element stringElem = getType("java.lang.String");
        Element objectElem = getType("java.lang.Object");
        boolean match = typeUtil.isAssignable(params.get(0).asType(), contextElem.asType());
        if (!match)
            return false;
        match = typeUtil.isAssignable(params.get(1).asType(), stringElem.asType());
        if (!match)
            return false;
        match = typeUtil.isAssignable(params.get(2).asType(), objectElem.asType());
        return match;
    }

    private ElementInformationContainer[] createEIC(List<Element> allMethods, Element spl) {
        ElementInformationContainer[] eics = new ElementInformationContainer[allMethods.size()];
        for (int i = 0; i < allMethods.size(); i++) {
            Element method = allMethods.get(i);
            String[] keys = method.getAnnotation(SPUpdateTarget.class).keys();
            eics[i] = new ElementInformationContainer(spl.toString(),
                    method.getSimpleName().toString(), keys);
        }
        return eics;
    }

    //----------------------------------------------------------------------------------------------

    private boolean ensureSingleSPM(Set<? extends Element> allSPMElements) {
        printNote("Amount of @SPM: " + allSPMElements.size());
        if (allSPMElements.size() <= 0)
            return false;
        else if (allSPMElements.size() == 1) {
            return true;
        } else {
            ArrayList<String> names = new ArrayList<>();
            for (Element spm : allSPMElements)
                names.add(spm.getSimpleName().toString());
            printError("There can not be more than 1 class annotated with @SPManager.\n" +
                    " The following classes are detected as being annotated:" +
                    names.toString());
            return false;
        }
    }

    /**
     * This method ensures that the annotated element is a singleton by making sure that it has a
     * private default (not args) constructor and a public static getter for the singleton.
     *
     * @param spm
     *         - the element in question.
     */
    private boolean ensureSingleton(Element spm) {
        //Check default constructor & singleton.
        boolean privateConstructor = false, foundOtherNonPrivateConstructor = false, staticFetcher = false;
        for (ExecutableElement constructor : ElementFilter.constructorsIn(spm.getEnclosedElements()))
            if (constructor.getModifiers().contains(Modifier.PRIVATE))
                privateConstructor = true;
            else {
                foundOtherNonPrivateConstructor = true;
                break;
            }
        if (!privateConstructor) {
            printError("No private default constructor found.", spm);
            return false;
        }
        if (foundOtherNonPrivateConstructor) {
            printError("Found non private constructor. It is okay to have multiple constructors in the shared preferences manager class, but they must be private.",
                    spm);
            return false;
        }

        for (ExecutableElement method : ElementFilter.methodsIn(spm.getEnclosedElements()))
            if (method.getModifiers().contains(Modifier.STATIC))
                //static method
                if (processingEnv.getTypeUtils().isAssignable(method.getReturnType(), spm.asType())) {
                    //static and returns same type.
                    staticFetcher = true;
                    break;
                }
        if (!staticFetcher) {
            printError("Couldn't manage to find a 'getInstance' type of method, i.e. a method that returns an instance of this singleton class.",
                    spm);
            return false;
        }
        return true;
    }

    //----------------------------------------------------------------------------------------------

    private TypeElement getType(String ref) {
        return processingEnv.getElementUtils().getTypeElement(ref);
    }

    private void printNote(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void printError(String msg) {
        printError(msg, null);
    }

    private void printError(String msg, Element elem) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "\n################################################\n" + msg + "\n################################################\n", elem);
    }
}
