package com.slooshfilm.app.data;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J\u0014\u0010\u0002\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u00040\u0003H&J\u0016\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u00032\u0006\u0010\u0007\u001a\u00020\bH&J\u0018\u0010\t\u001a\u0012\u0012\u000e\u0012\f\u0012\b\u0012\u00060\u0005j\u0002`\n0\u00040\u0003H&J\u001a\u0010\t\u001a\f\u0012\b\u0012\u00060\u0005j\u0002`\n0\u00032\u0006\u0010\u0007\u001a\u00020\bH&J\u0018\u0010\u000b\u001a\u0012\u0012\u000e\u0012\f\u0012\b\u0012\u00060\u0005j\u0002`\f0\u00040\u0003H&J\u001a\u0010\r\u001a\f\u0012\b\u0012\u00060\u0005j\u0002`\f0\u00032\u0006\u0010\u0007\u001a\u00020\bH&J\u001c\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u00040\u00032\u0006\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\u0010\u00c0\u0006\u0003"}, d2 = {"Lcom/slooshfilm/app/data/HdRezkaApi;", "", "getMovies", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/slooshfilm/app/data/model/Movie;", "getMovie", "id", "", "getSeries", "Lcom/slooshfilm/app/data/Series;", "getCartoons", "Lcom/slooshfilm/app/data/Cartoon;", "getCartoon", "getComments", "Lcom/slooshfilm/app/data/hdrezka/Comment;", "library_debug"})
public abstract interface HdRezkaApi {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.model.Movie>> getMovies();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.slooshfilm.app.data.model.Movie> getMovie(@org.jetbrains.annotations.NotNull()
    java.lang.String id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.model.Movie>> getSeries();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.slooshfilm.app.data.model.Movie> getSeries(@org.jetbrains.annotations.NotNull()
    java.lang.String id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.model.Movie>> getCartoons();
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.slooshfilm.app.data.model.Movie> getCartoon(@org.jetbrains.annotations.NotNull()
    java.lang.String id);
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.slooshfilm.app.data.hdrezka.Comment>> getComments(@org.jetbrains.annotations.NotNull()
    java.lang.String id);
}