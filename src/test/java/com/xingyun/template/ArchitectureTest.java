package com.xingyun.template;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 架构边界守卫：将 AGENTS.md 中的依赖方向与模块封装红线固化为可执行测试。
 *
 * <p>仅分析生产代码（{@code DoNotIncludeTests}），测试代码遵循测试域惯例。
 */
@AnalyzeClasses(
        packages = "com.xingyun.template",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String BASE = "com.xingyun.template";
    private static final String MODULES = BASE + ".module";

    /**
     * Rule 1：Domain 禁止依赖 Application / Infrastructure。
     *
     * <p>Domain 可依赖 JDK、Lombok（编译期增强）、{@code shared.exception.BusinessException}
     * 等共享支撑，但不得依赖同模块的应用层与基础设施层。
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers = noClasses()
            .that().resideInAPackage(MODULES + "..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    MODULES + "..application..",
                    MODULES + "..infrastructure.."
            );

    /**
     * Rule 2：Domain 禁止依赖技术框架。
     *
     * <p>锁定明显的技术边界（Spring、MyBatis、Jakarta Web/Validation/Servlet/Persistence），
     * 不建立庞大的第三方黑名单；Lombok 与 JSpecify 属编译期增强，明确放行。
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_technical_frameworks = noClasses()
            .that().resideInAPackage(MODULES + "..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "org.apache.ibatis..",
                    "com.baomidou..",
                    "jakarta.servlet..",
                    "jakarta.validation..",
                    "jakarta.persistence..",
                    "jakarta.ws.rs.."
            );

    /**
     * Rule 3：Application 禁止依赖 Infrastructure / Web。
     *
     * <p>Application 可依赖 Domain、本模块 api、shared 与其他模块的公开契约，
     * 但不得反向依赖基础设施层（含 Web、持久化、集成适配）。
     */
    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(MODULES + "..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    MODULES + "..infrastructure.."
            );

    /**
     * Rule 4：模块之间只能穿过公开契约（api 包 + 契约直接引用的稳定值类型）。
     *
     * <p>公开契约 = 对方模块的 api 包 ∪ 被 api 包直接引用的稳定值类型（record / enum）。
     * 例如 {@code ExampleApi} 签名引用 {@code ExampleId}（位于 domain.model），
     * 因此其他模块依赖 {@code ExampleId} 合法；但依赖 {@code ExampleModel}、
     * {@code ExampleRepository}、{@code ExampleApiImpl} 等内部实现必须失败。
     */
    @ArchTest
    void cross_module_dependencies_through_public_contract_only(JavaClasses classes) {
        classes().that().resideInAPackage(MODULES + "..")
                .should(new CrossModuleThroughPublicContractCondition(classes))
                .check(classes);
    }

    /**
     * Rule 5a：Repository 接口不得依赖 Infrastructure。
     *
     * <p>Repository 是领域持久化抽象（纯 Java Interface），实现位于 Infrastructure；
     * 接口不得反向依赖 Mapper、PO、Converter 等基础设施实现。
     */
    @ArchTest
    static final ArchRule repository_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(MODULES + "..domain.repository..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    MODULES + "..infrastructure.."
            );

    /**
     * Rule 5b：Port 接口不得依赖 Infrastructure。
     *
     * <p>Port 是应用层对外部能力的抽象（纯 Java Interface），实现位于 Infrastructure 的
     * Integration Adapter；接口不得携带 Vendor SDK 或基础设施类型。
     */
    @ArchTest
    static final ArchRule port_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage(MODULES + "..application.port..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    MODULES + "..infrastructure.."
            )
            .allowEmptyShould(true);

    /**
     * Rule 6：模块之间不得出现循环依赖。
     */
    @ArchTest
    static final ArchRule modules_must_be_free_of_cycles = slices()
            .matching(MODULES + ".(*)..")
            .should().beFreeOfCycles();

    /**
     * 跨模块只能通过对方公开契约依赖的条件。
     *
     * <p>对每个模块，预先计算其公开契约集合 = api 包的全部类 ∪ api 包直接引用的同模块稳定值类型。
     * 检查时，源模块对其他模块的任何依赖目标都必须落在目标模块的公开契约集合内。
     */
    private static final class CrossModuleThroughPublicContractCondition extends ArchCondition<JavaClass> {

        private final Map<String, Set<String>> publicContractByModule;

        CrossModuleThroughPublicContractCondition(JavaClasses classes) {
            super("只通过对方模块的公开契约（api 包 + 契约引用的稳定值类型）依赖其他模块");
            this.publicContractByModule = computePublicContracts(classes);
        }

        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            String sourceModule = moduleOf(javaClass).orElse(null);
            if (sourceModule == null) {
                return;
            }
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                String targetModule = moduleOf(target).orElse(null);
                if (targetModule == null || targetModule.equals(sourceModule)) {
                    continue;
                }
                Set<String> targetContract = publicContractByModule.getOrDefault(targetModule, Set.of());
                if (!targetContract.contains(target.getName())) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            javaClass.getName() + " 依赖了 " + target.getName()
                                    + "，该类不属于模块 " + targetModule + " 的公开契约"));
                }
            }
        }

        private static Map<String, Set<String>> computePublicContracts(JavaClasses classes) {
            Map<String, Set<String>> contracts = new HashMap<>();
            for (JavaClass candidate : classes) {
                Optional<String> moduleOpt = moduleOf(candidate);
                if (moduleOpt.isEmpty()) {
                    continue;
                }
                String module = moduleOpt.get();
                String apiPackage = MODULES + "." + module + ".api";
                String pkg = candidate.getPackageName();
                if (!pkg.equals(apiPackage) && !pkg.startsWith(apiPackage + ".")) {
                    continue;
                }
                Set<String> contract = contracts.computeIfAbsent(module, k -> new HashSet<>());
                contract.add(candidate.getName());
                for (Dependency dependency : candidate.getDirectDependenciesFromSelf()) {
                    JavaClass referenced = dependency.getTargetClass();
                    if (isStableValueType(referenced) && module.equals(moduleOf(referenced).orElse(null))) {
                        contract.add(referenced.getName());
                    }
                }
            }
            return contracts;
        }

        private static Optional<String> moduleOf(JavaClass javaClass) {
            String pkg = javaClass.getPackageName();
            String prefix = MODULES + ".";
            if (!pkg.startsWith(prefix)) {
                return Optional.empty();
            }
            String rest = pkg.substring(prefix.length());
            int dot = rest.indexOf('.');
            return Optional.of(dot > 0 ? rest.substring(0, dot) : rest);
        }

        private static boolean isStableValueType(JavaClass javaClass) {
            return javaClass.isEnum() || javaClass.isRecord();
        }
    }
}
