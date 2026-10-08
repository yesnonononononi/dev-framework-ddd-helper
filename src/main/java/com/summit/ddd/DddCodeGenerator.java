package com.summit.ddd;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * DDD 模块代码生成器
 * <p>按 {@code resources/template} 下的模板为一个业务模块生成整套分层骨架：
 * api(controller/request/feign)、application(service/serviceImpl/command/vo)、
 * domain(model/repository/batchQueryEvent)、infrastructure(repository/mapper/po)。</p>
 * <p>用法（项目根目录执行，模板先经 {@code mvn compile} 拷贝到 target/classes）：</p>
 * <pre>
 *   mvn -q clean compile
 *   # 无基础包：生成到 src/main/java/user/...，包名 user.*
 *   java -cp target/classes com.summit.ddd.DddCodeGenerator user [--force]
 *   # 指定基础包：生成到 src/main/java/com/summit/dp/shared/user/...，包名 com.summit.dp.shared.user.*
 *   java -cp target/classes com.summit.ddd.DddCodeGenerator user --base-package com.summit.dp.shared
 * </pre>
 * <p>占位符：{@code [PACKAGE]} 当前文件完整包名，{@code [MODULE_PACKAGE]} 模块根包名（含基础包，保留原始大小写，
 * 如 com.summit.dp.shared.commonConfig），{@code [HOLDER]} 帕斯卡命名（取模块名末段，如 User、CommonConfig），
 * {@code [holder]} 模块名末段小写（如 user、commonconfig，用于 URL 路径与表名）。</p>
 */
public final class DddCodeGenerator {

    /** 生成目标类型：类名后缀、相对模块根包的目录、classpath 模板路径 */
    private enum Type {
        CONTROLLER("Controller", "api/controller", "template/api/controller.txt"),
        REQUEST("Request", "api/request", "template/api/request.txt"),
        FEIGN("Feign", "api/feign", "template/api/feign.txt"),
        COMMAND("Command", "application/command", "template/application/command.txt"),
        SERVICE("Service", "application/service", "template/application/service.txt"),
        SERVICE_IMPL("ServiceImpl", "application/service/impl", "template/application/serviceImpl.txt"),
        VO("VO", "application/vo", "template/application/vo.txt"),
        MODEL("", "domain/model", "template/domain/model.txt"),
        REPOSITORY("Repository", "domain/repository", "template/domain/repository.txt"),
        BATCH_QUERY_EVENT("BatchQueryEvent", "domain/event", "template/domain/batchQueryEvent.txt"),
        REPOSITORY_IMPL("RepositoryImpl", "infrastructure/repository", "template/infrastructure/repositoryImpl.txt"),
        PO("PO", "infrastructure/persistence/po", "template/infrastructure/po.txt"),
        MAPPER("Mapper", "infrastructure/persistence/mapper", "template/infrastructure/mapper.txt");

        private final String suffix;
        private final String dir;
        private final String template;

        Type(String suffix, String dir, String template) {
            this.suffix = suffix;
            this.dir = dir;
            this.template = template;
        }
    }

    private DddCodeGenerator() {
    }

    public static void main(String[] args) throws IOException {
        String module = null;
        String basePackage = "";
        boolean force = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--force".equals(arg)) {
                force = true;
            } else if ("--base-package".equals(arg) || "-p".equals(arg)) {
                if (i + 1 >= args.length)
                    throw new IllegalArgumentException("missing value for " + arg);
                basePackage = args[++i];
            } else if (arg.startsWith("--base-package=")) {
                basePackage = arg.substring("--base-package=".length());
            } else if (module == null) {
                module = arg;
            } else {
                throw new IllegalArgumentException("unknown argument: " + arg);
            }
        }
        if (module == null)
            module = "user";
        Path baseDir = Paths.get(System.getProperty("user.dir"), "src", "main", "java");
        int count = generate(module, basePackage, baseDir, force);
        System.out.println("module '" + module + "' generated " + count + " file(s)"
                + (force ? " (force)" : ""));
    }

    /**
     * 为模块生成整套分层骨架代码（无基础包前缀）
     *
     * @param module  模块名，如 "user"，将生成到 {@code baseDir/user/...}，包名 {@code user.*}
     * @param baseDir 源码根目录，如 {@code src/main/java}
     * @param force   为 true 时覆盖已存在文件，否则跳过
     * @return 生成/覆盖的文件数
     */
    public static int generate(String module, Path baseDir, boolean force) throws IOException {
        return generate(module, "", baseDir, force);
    }

    /**
     * 为模块生成整套分层骨架代码
     *
     * @param module      模块名，可含包路径，如 "user" 或 "shared.user"；类名与 URL 取最后一段
     * @param basePackage 基础包名，如 "com.summit.dp.shared"，会拼在 {@code module} 之前；
     *                    传空表示无前缀
     * @param baseDir     源码根目录，如 {@code src/main/java}
     * @param force       为 true 时覆盖已存在文件，否则跳过
     * @return 生成/覆盖的文件数
     */
    public static int generate(String module, String basePackage, Path baseDir, boolean force) throws IOException {
        String modulePath = module == null ? "" : module.trim();
        if (modulePath.isEmpty())
            throw new IllegalArgumentException("module must not be blank");
        String prefix = basePackage == null ? "" : basePackage.trim();
        String fullPkg = prefix.isEmpty() ? modulePath : prefix + "." + modulePath;

        String name = fullPkg.substring(fullPkg.lastIndexOf('.') + 1);
        String holder = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String holderLower = name.toLowerCase();
        String pkgDir = fullPkg.replace('.', '/');

        int count = 0;
        for (Type type : Type.values()) {
            String filePkg = fullPkg + "." + type.dir.replace('/', '.');
            String className = holder + type.suffix;
            String code = render(readTemplate(type.template), filePkg, fullPkg, holder, holderLower);

            Path file = baseDir.resolve(pkgDir).resolve(type.dir).resolve(className + ".java");
            if (Files.exists(file) && !force) {
                System.out.println("  skip  " + file);
                continue;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, code, StandardCharsets.UTF_8);
            System.out.println("  write " + file);
            count++;
        }
        return count;
    }

    private static String readTemplate(String resource) throws IOException {
        try (InputStream in = DddCodeGenerator.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null)
                throw new IOException("template not found on classpath: " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String render(String template, String filePkg, String modulePkg, String holder, String holderLower) {
        return template
                .replace("[PACKAGE]", filePkg)
                .replace("[MODULE_PACKAGE]", modulePkg)
                .replace("[HOLDER]", holder)
                .replace("[holder]", holderLower);
    }
}
