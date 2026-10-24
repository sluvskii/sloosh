package com.slooshfilm.app.data.hdrezka;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\r\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cJ\u0016\u0010\u000e\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\fJ\u0018\u0010\u001e\u001a\u00020\u001a2\b\u0010\u0011\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u001b\u001a\u00020\u001cJ>\u0010\u001f\u001a\u00020\u001a2\b\u0010 \u001a\u0004\u0018\u00010\u00052\b\u0010!\u001a\u0004\u0018\u00010\u00052\b\u0010\"\u001a\u0004\u0018\u00010\u00052\b\u0010#\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010$\u001a\u00020\fJ8\u0010%\u001a\u00020\u001a2\b\u0010 \u001a\u0004\u0018\u00010\u00052\b\u0010!\u001a\u0004\u0018\u00010\u00052\b\u0010\"\u001a\u0004\u0018\u00010\u00052\b\u0010#\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u0010\u0010&\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u000e\u0010\'\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cJ\u0010\u0010(\u001a\u00020\u00052\u0006\u0010)\u001a\u00020*H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0086\u000e\u00a2\u0006\u0010\n\u0002\u0010\u0010\u001a\u0004\b\u000b\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001c\u0010\u0011\u001a\u0004\u0018\u00010\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001c\u0010\u0016\u001a\u0004\u0018\u00010\u0005X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0013\"\u0004\b\u0018\u0010\u0015\u00a8\u0006+"}, d2 = {"Lcom/slooshfilm/app/data/hdrezka/UserData;", "", "<init>", "()V", "USER_ID", "", "USER_NAME", "USER_HASH", "SESSION_ID", "IS_LOGGED_IN", "USER_AVATAR", "isLoggedIn", "", "()Ljava/lang/Boolean;", "setLoggedIn", "(Ljava/lang/Boolean;)V", "Ljava/lang/Boolean;", "avatarLink", "getAvatarLink", "()Ljava/lang/String;", "setAvatarLink", "(Ljava/lang/String;)V", "userName", "getUserName", "setUserName", "init", "", "context", "Landroid/content/Context;", "loggedIn", "setAvatar", "setCookies", "user_id", "user_name", "password", "session", "isSave", "saveCookies", "loadCookies", "reset", "getProvider", "localStorage", "Lcom/slooshfilm/app/data/LocalStorage;", "library_debug"})
public final class UserData {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_ID = "user_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_NAME = "user_name";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_HASH = "user_hash";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SESSION_ID = "session_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String IS_LOGGED_IN = "is_logged_in";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String USER_AVATAR = "user_avatar";
    @org.jetbrains.annotations.Nullable()
    private static java.lang.Boolean isLoggedIn;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String avatarLink;
    @org.jetbrains.annotations.Nullable()
    private static java.lang.String userName;
    @org.jetbrains.annotations.NotNull()
    public static final com.slooshfilm.app.data.hdrezka.UserData INSTANCE = null;
    
    private UserData() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Boolean isLoggedIn() {
        return null;
    }
    
    public final void setLoggedIn(@org.jetbrains.annotations.Nullable()
    java.lang.Boolean p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAvatarLink() {
        return null;
    }
    
    public final void setAvatarLink(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getUserName() {
        return null;
    }
    
    public final void setUserName(@org.jetbrains.annotations.Nullable()
    java.lang.String p0) {
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void setLoggedIn(@org.jetbrains.annotations.NotNull()
    android.content.Context context, boolean loggedIn) {
    }
    
    public final void setAvatar(@org.jetbrains.annotations.Nullable()
    java.lang.String avatarLink, @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void setCookies(@org.jetbrains.annotations.Nullable()
    java.lang.String user_id, @org.jetbrains.annotations.Nullable()
    java.lang.String user_name, @org.jetbrains.annotations.Nullable()
    java.lang.String password, @org.jetbrains.annotations.Nullable()
    java.lang.String session, @org.jetbrains.annotations.NotNull()
    android.content.Context context, boolean isSave) {
    }
    
    private final void saveCookies(java.lang.String user_id, java.lang.String user_name, java.lang.String password, java.lang.String session, android.content.Context context) {
    }
    
    private final void loadCookies(android.content.Context context) {
    }
    
    public final void reset(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    private final java.lang.String getProvider(com.slooshfilm.app.data.LocalStorage localStorage) {
        return null;
    }
}