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
 * api(controller)、application(service/request/command/vo)、domain(model/repository)、
 * infrastructure(repository/mapper/po)。</p>
 * <p>用法（项目根目录执行，模板先经 {@code mvn compile} 拷贝到 target/classes）：</p>
 * <pre>
 *   mvn -q clean compile
 *   java -cp target/classes com.summit.ddd.DddCodeGenerator user [--force]
 * </pre>
 * <p>占位符：{@code [PACKAGE]} 当前文件完整包名，{@code [MODULE_PACKAGE]} 模块根包名，
 * {@code [HOLDER]} 帕斯卡命名（如 User），{@code [holder]} 模块名小写（如 user）。</p>
 */
public final class DddCodeGenerator {

    /** 生成目标类型：类名后缀、相对模块根包的目录、classpath 模板路径 */
    private enum Type {
        CONTROLLER("Controller", "api/controller", "template/api/controller.txt"),
        REQUEST("Request", "api/request", "template/api/request.txt"),
        COMMAND("Command", "application/command", "template/application/command.txt"),
        SERVICE("Service", "application/service", "template/application/service.txt"),
        SERVICE_IMPL("ServiceImpl", "application/service/impl", "template/application/serviceImpl.txt"),
        VO("VO", "application/vo", "template/application/vo.txt"),
        MODEL("", "domain/model", "template/domain/model.txt"),
        REPOSITORY("Repository", "domain/repository", "template/domain/repository.txt"),
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
        String module = args.length > 0 ? args[0] : "user";
        boolean force = args.length > 1 && "--force".equals(args[1]);
        Path baseDir = Paths.get(System.getProperty("user.dir"), "src", "main", "java");
        int count = generate(module, baseDir, force);
        System.out.println("module '" + module + "' generated " + count + " file(s)"
                + (force ? " (force)" : ""));
    }

    /**
     * 为模块生成整套分层骨架代码
     *
     * @param module  模块名，如 "user"，将生成到 {@code baseDir/user/...}，包名 {@code user.*}
     * @param baseDir 源码根目录，如 {@code src/main/java}
     * @param force   为 true 时覆盖已存在文件，否则跳过
     * @return 生成/覆盖的文件数
     */
    public static int generate(String module, Path baseDir, boolean force) throws IOException {
        String modulePkg = module.trim().toLowerCase();
        if (modulePkg.isEmpty())
            throw new IllegalArgumentException("module must not be blank");
        String holder = Character.toUpperCase(modulePkg.charAt(0)) + modulePkg.substring(1);

        int count = 0;
        for (Type type : Type.values()) {
            String filePkg = modulePkg + "." + type.dir.replace('/', '.');
            String className = holder + type.suffix;
            String code = render(readTemplate(type.template), filePkg, modulePkg, holder);

            Path file = baseDir.resolve(modulePkg).resolve(type.dir).resolve(className + ".java");
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

    private static String render(String template, String filePkg, String modulePkg, String holder) {
        return template
                .replace("[PACKAGE]", filePkg)
                .replace("[MODULE_PACKAGE]", modulePkg)
                .replace("[HOLDER]", holder)
                .replace("[holder]", modulePkg);
    }
}
