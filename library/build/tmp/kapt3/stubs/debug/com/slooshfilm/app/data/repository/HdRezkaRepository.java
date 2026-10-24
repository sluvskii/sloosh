package com.slooshfilm.app.data.repository;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u00ae\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0014\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u000e\u0010#\u001a\u00020$H\u0082@\u00a2\u0006\u0002\u0010%J\b\u0010&\u001a\u00020$H\u0002J\u0010\u0010\'\u001a\u00020\u00182\u0006\u0010(\u001a\u00020\u001fH\u0002J8\u0010)\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u001b\u0012\u0004\u0012\u00020\u001e0\u00192\u0006\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020\u001eH\u0086@\u00a2\u0006\u0002\u0010/J\u000e\u00100\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010%J4\u00101\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001b2\u0006\u00102\u001a\u00020\u001c2\n\b\u0002\u00103\u001a\u0004\u0018\u00010\u00182\n\b\u0002\u00104\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u00105J\u001e\u00106\u001a\u00020\u00102\u0006\u00107\u001a\u00020\u00182\u0006\u00108\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u00109J\u0016\u0010:\u001a\u00020\u00102\u0006\u00107\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u0016\u0010<\u001a\u00020\u00102\u0006\u0010=\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u001c\u0010>\u001a\b\u0012\u0004\u0012\u00020\u001f0\u001b2\u0006\u0010?\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u001c\u0010@\u001a\b\u0012\u0004\u0012\u00020\u00180\u001b2\u0006\u0010?\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u0018\u0010A\u001a\u0004\u0018\u00010B2\u0006\u0010C\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J<\u0010D\u001a\u0014\u0012\u0004\u0012\u00020\u001e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0\u001b0!2\u0006\u0010E\u001a\u00020\u00182\u0006\u0010F\u001a\u00020\u00182\n\b\u0002\u0010G\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u0010HJ6\u0010I\u001a\u00020J2\u0006\u0010E\u001a\u00020\u00182\u0006\u0010F\u001a\u00020\u00182\n\b\u0002\u0010K\u001a\u0004\u0018\u00010\u00182\n\b\u0002\u0010L\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u0010MJ\u0014\u0010N\u001a\b\u0012\u0004\u0012\u00020O0\u001bH\u0086@\u00a2\u0006\u0002\u0010%J\u001e\u0010P\u001a\u00020\u00102\u0006\u0010P\u001a\u00020\u00182\u0006\u0010Q\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u00109J\b\u0010R\u001a\u0004\u0018\u00010\u0018J\u0006\u0010S\u001a\u00020$J*\u0010T\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u001b\u0012\u0004\u0012\u00020\u001e0\u00192\b\b\u0002\u0010.\u001a\u00020\u001eH\u0086@\u00a2\u0006\u0002\u0010UJ\u0016\u0010V\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u001e\u0010V\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u00182\u0006\u00107\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u00109J\u0016\u0010W\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u001e\u0010W\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u00182\u0006\u00107\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u00109J(\u0010X\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u001c\u0012\u0004\u0012\u00020\u00100\u00190\u001b2\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u000e\u0010Y\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018J\u0016\u0010Z\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u0016\u0010[\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u0016\u0010\\\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J\u0016\u0010]\u001a\u00020\u00102\u0006\u0010E\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J2\u0010^\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020_0\u001b\u0012\u0004\u0012\u00020\u001e0\u00192\u0006\u0010E\u001a\u00020\u00182\b\b\u0002\u0010.\u001a\u00020\u001eH\u0086@\u00a2\u0006\u0002\u0010`J\"\u0010a\u001a\u00020\u001e2\u0006\u0010E\u001a\u00020\u00182\n\b\u0002\u0010b\u001a\u0004\u0018\u00010\u0018H\u0086@\u00a2\u0006\u0002\u00109J\u0018\u0010c\u001a\u0004\u0018\u00010d2\u0006\u0010e\u001a\u00020\u0018H\u0086@\u00a2\u0006\u0002\u0010;J:\u0010f\u001a\u00020$2\u0006\u0010E\u001a\u00020\u00182\u0006\u0010F\u001a\u00020\u00182\b\u0010K\u001a\u0004\u0018\u00010\u00182\b\u0010L\u001a\u0004\u0018\u00010\u00182\u0006\u0010g\u001a\u00020\u001aH\u0086@\u00a2\u0006\u0002\u0010hR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00100\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0013R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0015X\u0082\u000e\u00a2\u0006\u0002\n\u0000R8\u0010\u0016\u001a,\u0012\u0004\u0012\u00020\u0018\u0012\"\u0012 \u0012\u0004\u0012\u00020\u001a\u0012\u0016\u0012\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u001c\u0012\u0004\u0012\u00020\u00100\u00190\u001b0\u00190\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u001d\u001a\u0014\u0012\u0004\u0012\u00020\u001e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001f0\u001b0\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R,\u0010 \u001a \u0012\u0004\u0012\u00020\u0018\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u001e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\"0\u001b0!0\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006i"}, d2 = {"Lcom/slooshfilm/app/data/repository/HdRezkaRepository;", "", "localStorage", "Lcom/slooshfilm/app/data/LocalStorage;", "<init>", "(Lcom/slooshfilm/app/data/LocalStorage;)V", "universalParser", "Lcom/slooshfilm/app/data/parser/UniversalParser;", "configProvider", "Lcom/slooshfilm/app/data/parser/ConfigProvider;", "api", "Lcom/slooshfilm/app/data/hdrezka/HdRezkaApi;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "_isLoggedIn", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "isLoggedIn", "Lkotlinx/coroutines/flow/StateFlow;", "()Lkotlinx/coroutines/flow/StateFlow;", "bookmarksPageCache", "Lcom/slooshfilm/app/data/models/BookmarksPage;", "membershipCache", "", "", "Lkotlin/Pair;", "", "", "Lcom/slooshfilm/app/data/models/Bookmark;", "watchLaterCache", "", "Lcom/slooshfilm/app/data/model/Movie;", "episodesCache", "", "Lcom/slooshfilm/app/data/model/Episode;", "loadCaches", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "invalidateCaches", "parseFilmType", "movie", "getContent", "contentType", "Lcom/slooshfilm/app/data/repository/ContentType;", "category", "Lcom/slooshfilm/app/data/repository/ContentCategory;", "page", "(Lcom/slooshfilm/app/data/repository/ContentType;Lcom/slooshfilm/app/data/repository/ContentCategory;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getBookmarksPage", "getFilmsForBookmark", "bookmark", "sort", "genre", "(Lcom/slooshfilm/app/data/models/Bookmark;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "renameBookmarkCategory", "catId", "newName", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteBookmarkCategory", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "createBookmarkCategory", "name", "search", "query", "getSearchSuggestions", "getMovieDetails", "Lcom/slooshfilm/app/data/model/MovieDetails;", "url", "getEpisodes", "postId", "translatorId", "refererUrl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getStreamsForPost", "Lcom/slooshfilm/app/data/hdrezka/StreamInfo;", "season", "episode", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getNotifications", "Lcom/slooshfilm/app/data/hdrezka/Notification;", "login", "password", "getSavedUserName", "logout", "getWatchLater", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "addBookmark", "removeBookmark", "getBookmarkMembership", "isBookmarkedCached", "addWatchLater", "removeWatchLater", "isBookmarked", "isInWatchLater", "getComments", "Lcom/slooshfilm/app/data/hdrezka/Comment;", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCommentsCount", "movieUrl", "getActorDetails", "Lcom/slooshfilm/app/data/model/ActorDetails;", "link", "updateTime", "timeInSeconds", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "library_debug"})
public final class HdRezkaRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.LocalStorage localStorage = null;
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.parser.UniversalParser universalParser = null;
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.parser.ConfigProvider configProvider = null;
    @org.jetbrains.annotations.NotNull()
    private final com.slooshfilm.app.data.hdrezka.HdRezkaApi api = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoggedIn = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoggedIn = null;
    @org.jetbrains.annotations.Nullable()
    private com.slooshfilm.app.data.models.BookmarksPage bookmarksPageCache;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, kotlin.Pair<java.lang.Long, java.util.List<kotlin.Pair<com.slooshfilm.app.data.models.Bookmark, java.lang.Boolean>>>> membershipCache = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.Integer, java.util.List<com.slooshfilm.app.data.model.Movie>> watchLaterCache = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.Map<java.lang.Integer, java.util.List<com.slooshfilm.app.data.model.Episode>>> episodesCache = null;
    
    public HdRezkaRepository(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.LocalStorage localStorage) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoggedIn() {
        return null;
    }
    
    private final java.lang.Object loadCaches(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void invalidateCaches() {
    }
    
    private final java.lang.String parseFilmType(com.slooshfilm.app.data.model.Movie movie) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getContent(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.repository.ContentType contentType, @org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.repository.ContentCategory category, int page, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Pair<? extends java.util.List<com.slooshfilm.app.data.model.Movie>, java.lang.Integer>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getBookmarksPage(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.models.BookmarksPage> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getFilmsForBookmark(@org.jetbrains.annotations.NotNull()
    com.slooshfilm.app.data.models.Bookmark bookmark, @org.jetbrains.annotations.Nullable()
    java.lang.String sort, @org.jetbrains.annotations.Nullable()
    java.lang.String genre, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.slooshfilm.app.data.model.Movie>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object renameBookmarkCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String catId, @org.jetbrains.annotations.NotNull()
    java.lang.String newName, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteBookmarkCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String catId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object createBookmarkCategory(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object search(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.slooshfilm.app.data.model.Movie>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSearchSuggestions(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getMovieDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.model.MovieDetails> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getEpisodes(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    java.lang.String translatorId, @org.jetbrains.annotations.Nullable()
    java.lang.String refererUrl, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.Integer, ? extends java.util.List<com.slooshfilm.app.data.model.Episode>>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getStreamsForPost(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    java.lang.String translatorId, @org.jetbrains.annotations.Nullable()
    java.lang.String season, @org.jetbrains.annotations.Nullable()
    java.lang.String episode, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.hdrezka.StreamInfo> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getNotifications(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.slooshfilm.app.data.hdrezka.Notification>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object login(@org.jetbrains.annotations.NotNull()
    java.lang.String login, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    /**
     * Returns username saved in preferences (if any).
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSavedUserName() {
        return null;
    }
    
    public final void logout() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getWatchLater(int page, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Pair<? extends java.util.List<com.slooshfilm.app.data.model.Movie>, java.lang.Integer>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addBookmark(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addBookmark(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    java.lang.String catId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object removeBookmark(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    /**
     * Remove a bookmark from a specific category.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object removeBookmark(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    java.lang.String catId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    /**
     * Returns list of pairs (Bookmark, containsPost) for the current user's bookmark categories.
     * This loads each category page and checks whether the given postId exists in that category.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getBookmarkMembership(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<kotlin.Pair<com.slooshfilm.app.data.models.Bookmark, java.lang.Boolean>>> $completion) {
        return null;
    }
    
    /**
     * Fast cached check for bookmark presence using local bookmarks page cache (non-blocking).
     */
    public final boolean isBookmarkedCached(@org.jetbrains.annotations.NotNull()
    java.lang.String postId) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object addWatchLater(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object removeWatchLater(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object isBookmarked(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object isInWatchLater(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getComments(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, int page, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Pair<? extends java.util.List<com.slooshfilm.app.data.hdrezka.Comment>, java.lang.Integer>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getCommentsCount(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.Nullable()
    java.lang.String movieUrl, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getActorDetails(@org.jetbrains.annotations.NotNull()
    java.lang.String link, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.slooshfilm.app.data.model.ActorDetails> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateTime(@org.jetbrains.annotations.NotNull()
    java.lang.String postId, @org.jetbrains.annotations.NotNull()
    java.lang.String translatorId, @org.jetbrains.annotations.Nullable()
    java.lang.String season, @org.jetbrains.annotations.Nullable()
    java.lang.String episode, long timeInSeconds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}