package com.aongoltzcrank.sharedpreferencesmanager.processors;

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager")
public class SPManagerProcessor extends AbstractProcessor {

    static final boolean debug = false;

    static final String defaultSPMName = "com.sharedpreferencesmanager.generated.SharedPreferencesManager";
    private static final String annotationRef = "com.aongoltzcrank.sharedpreferencesmanager.annotations.SPManager";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement spmTypeElement = processingEnv.getElementUtils().getTypeElement(annotationRef);
        Set<? extends Element> allSPMAnnotatedElements = roundEnv.getElementsAnnotatedWith(spmTypeElement);
        ensureSPMgrRequirements(allSPMAnnotatedElements);
        //TODO: add potential overrides for this function.
        return false;
    }

    /**
     * This function makes sure that any class annotated with @SharedPreferenceManager, is a singleton,
     * as well as that there is only one of them.
     *
     * @param allSPMAnnotatedElements
     *         - all classes that are annotated with @SharedPreferencesManager.
     */
    private void ensureSPMgrRequirements(Set<? extends Element> allSPMAnnotatedElements) {
        if (allSPMAnnotatedElements.size() > 1) {
            ArrayList<String> names = new ArrayList<>();
            for (Element element : allSPMAnnotatedElements)
                names.add(element.getSimpleName().toString());
            processingEnv.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR,
                            "There can not be more than 1 class annotated with @SharedPreferencesManager.\n" +
                                    " The following classes are detected as being annotated:" +
                                    names.toString());
        } else if (allSPMAnnotatedElements.size() == 1) {
            //Check default constructor & singleton.
            Element spm = allSPMAnnotatedElements.iterator().next();
            boolean privateConstructor = false;
            boolean foundOtherNonPrivateConstructor = false;
            boolean staticFetcher = false;
            for (ExecutableElement constructor : ElementFilter.constructorsIn(spm.getEnclosedElements())) {
                if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                    //private.
                    privateConstructor = true;
                } else {
                    foundOtherNonPrivateConstructor = true;
                    break;
                }
            }
            if (!privateConstructor)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "No private default constructor found.", spm);
            if (foundOtherNonPrivateConstructor)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Found non private constructor. It is okay to have multiple constructors in the shared preferences manager class, but they must be private.",
                        spm);

            for (ExecutableElement method : ElementFilter.methodsIn(spm.getEnclosedElements()))
                if (method.getModifiers().contains(Modifier.STATIC))
                    //static method
                    if (processingEnv.getTypeUtils().isAssignable(method.getReturnType(), spm.asType())) {
                        //static and returns same type.
                        staticFetcher = true;
                        break;
                    }
            if (!staticFetcher)
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Couldn't manage to find a 'getInstance' type of method, i.e. a method that returns an instance of this singleton class.",
                        spm);

        }
    }
}
