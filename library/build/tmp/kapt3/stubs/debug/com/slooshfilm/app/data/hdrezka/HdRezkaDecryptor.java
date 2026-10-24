package com.slooshfilm.app.data.hdrezka;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\f\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0011\u001a\u00020\u0005J\u0010\u0010\u0012\u001a\u00020\u00052\u0006\u0010\u0013\u001a\u00020\u0005H\u0002J\u001e\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u00072\u0006\u0010\u0013\u001a\u00020\u00052\u0006\u0010\u0016\u001a\u00020\u0005H\u0002J*\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00050\u00182\u0006\u0010\u0013\u001a\u00020\u00052\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\t0\u0007H\u0002J\u0016\u0010\u001a\u001a\u00020\u000b2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H\u0002J$\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00050\u00072\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00050\u00072\u0006\u0010\u001c\u001a\u00020\u0015H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006\u001d"}, d2 = {"Lcom/slooshfilm/app/data/hdrezka/HdRezkaDecryptor;", "", "<init>", "()V", "TRASH_SEPARATOR", "", "TRASH_SYMBOLS", "", "CLEAR_BEFORE_SYMBOLS", "", "trashRegex", "Lkotlin/text/Regex;", "getTrashRegex", "()Lkotlin/text/Regex;", "trashRegex$delegate", "Lkotlin/Lazy;", "decrypt", "encrypted", "decryptRecursive", "input", "indexesOf", "", "search", "divideAtFirstOccurrenceOfSymbols", "Lkotlin/Pair;", "symbols", "generateTrashRegExp", "cartesianProduct", "n", "library_debug"})
public final class HdRezkaDecryptor {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TRASH_SEPARATOR = "//_//";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> TRASH_SYMBOLS = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.Character> CLEAR_BEFORE_SYMBOLS = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy trashRegex$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.slooshfilm.app.data.hdrezka.HdRezkaDecryptor INSTANCE = null;
    
    private HdRezkaDecryptor() {
        super();
    }
    
    private final kotlin.text.Regex getTrashRegex() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String decrypt(@org.jetbrains.annotations.NotNull()
    java.lang.String encrypted) {
        return null;
    }
    
    private final java.lang.String decryptRecursive(java.lang.String input) {
        return null;
    }
    
    private final java.util.List<java.lang.Integer> indexesOf(java.lang.String input, java.lang.String search) {
        return null;
    }
    
    private final kotlin.Pair<java.lang.String, java.lang.String> divideAtFirstOccurrenceOfSymbols(java.lang.String input, java.util.List<java.lang.Character> symbols) {
        return null;
    }
    
    private final kotlin.text.Regex generateTrashRegExp(java.util.List<java.lang.String> symbols) {
        return null;
    }
    
    private final java.util.List<java.lang.String> cartesianProduct(java.util.List<java.lang.String> symbols, int n) {
        return null;
    }
}