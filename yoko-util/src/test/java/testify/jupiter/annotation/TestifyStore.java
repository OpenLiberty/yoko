package testify.jupiter.annotation;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

public enum TestifyStore {
    ;
    private static final Namespace TESTIFY_NS = Namespace.create("testify");
    private static final Namespace TESTIFY_CLEANUP_NS = Namespace.create("testify", "cleanup");
    public static Store get(ExtensionContext ctx) { return ctx.getStore(TESTIFY_NS); }
}
