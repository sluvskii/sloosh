package com.slooshfilm.app.data.hdrezka;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\t\u001a\u00020\nJ\u0010\u0010\u000b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\t\u001a\u00020\nJ\u001e\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\nJ&\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\u00052\u0006\u0010\u0012\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u00052\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/slooshfilm/app/data/hdrezka/UserModel;", "", "<init>", "()V", "USER_PAGE", "", "LOGIN_AJAX", "REGISTER_AJAX", "getUserAvatarLink", "context", "Landroid/content/Context;", "getUserName", "login", "", "name", "password", "register", "email", "username", "library_debug"})
public final class UserModel {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_PAGE = "/user/";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LOGIN_AJAX = "/ajax/login/";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String REGISTER_AJAX = "/engine/ajax/quick_register.php";
    @org.jetbrains.annotations.NotNull()
    public static final com.slooshfilm.app.data.hdrezka.UserModel INSTANCE = null;
    
    private UserModel() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserAvatarLink(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserName(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    public final void login(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void register(@org.jetbrains.annotations.NotNull()
    java.lang.String email, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
}