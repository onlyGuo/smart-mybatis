package ink.icoding.smartmybatis.apt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * 为标注了 @SmartMeta 的 PO 子类生成元数据类型，按实体单文件输出到 .M 子包。
 */
@SupportedAnnotationTypes(PoDescriptionProcessor.GENERATE_META_ANNOTATION)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PoDescriptionProcessor extends AbstractProcessor {

    private static final String PO_TYPE = "ink.icoding.smartmybatis.entity.po.PO";
    static final String GENERATE_META_ANNOTATION = "ink.icoding.smartmybatis.entity.po.enums.SmartMeta";
    private static final String META_SUBPACKAGE = ".M";
    private static final String META_PREFIX = "$";

    private final Set<String> generatedTypeNames = new HashSet<>();

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement poType = elementUtils.getTypeElement(PO_TYPE);
        TypeElement markerAnnotationType = elementUtils.getTypeElement(GENERATE_META_ANNOTATION);
        if (poType == null || markerAnnotationType == null) {
            return false;
        }

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(markerAnnotationType);
        for (Element element : annotatedElements) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            if (!isPoSubclass(typeElement, poType)) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "@SmartMeta only supports PO subclasses: " + typeElement.getQualifiedName(),
                        typeElement);
                continue;
            }
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }
            if (!generatedTypeNames.add(typeElement.getQualifiedName().toString())) {
                continue;
            }
            generateMetaType(typeElement);
        }
        return false;
    }

    private boolean isPoSubclass(TypeElement candidate, TypeElement poType) {
        return typeUtils.isAssignable(candidate.asType(), poType.asType());
    }

    private void generateMetaType(TypeElement poType) {
        PackageElement packageElement = elementUtils.getPackageOf(poType);
        String poPackage = packageElement.isUnnamed() ? "" : packageElement.getQualifiedName().toString();
        String metaPackage = poPackage + META_SUBPACKAGE;
        String poSimpleName = poType.getSimpleName().toString();
        String metaSimpleName = META_PREFIX + poSimpleName;
        String poQualifiedName = poType.getQualifiedName().toString();
        String qualifiedName = metaPackage + "." + metaSimpleName;

        StringBuilder source = new StringBuilder();
        source.append("package ").append(metaPackage).append(";\n\n");
        source.append("import ink.icoding.smartmybatis.utils.entity.ColumnDeclaration;\n");
        source.append("import ink.icoding.smartmybatis.utils.entity.MapperDeclaration;\n");
        source.append("import ink.icoding.smartmybatis.utils.entity.MapperUtil;\n");
        source.append("import ink.icoding.smartmybatis.utils.entity.apt.SmartDeclaration;\n\n");

        source.append("public interface ").append(metaSimpleName)
                .append(" extends SmartDeclaration<").append(poQualifiedName).append("> {\n");
        source.append("    MapperDeclaration INSTANCE = MapperUtil.buildMapperDeclarationByPoClass(")
                .append(poQualifiedName).append(".class);\n");

        for (Element enclosedElement : poType.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.FIELD) {
                continue;
            }
            if (enclosedElement.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            String fieldName = enclosedElement.getSimpleName().toString();
            source.append("    ColumnDeclaration ").append(fieldName)
                    .append(" = MapperUtil.getFieldDeclarationByPoClass(")
                    .append(poQualifiedName).append(".class, \"")
                    .append(fieldName).append("\");\n");
        }
        source.append("}\n");

        try {
            JavaFileObject sourceFile = filer.createSourceFile(qualifiedName, poType);
            try (Writer writer = sourceFile.openWriter()) {
                writer.write(source.toString());
            }
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate meta type for " + poQualifiedName + ": " + e.getMessage());
        }
    }
}
