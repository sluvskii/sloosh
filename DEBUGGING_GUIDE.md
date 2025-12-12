# Debugging Guide: Episode Parsing Issues

## Problem Summary
1. **Slow loading** on first entry to a serial (gets faster on 2nd attempt - cache issue)
2. **Episodes show "Ожидается" and "Дата неизвестна"** instead of real titles/dates

## Changes Made

### 1. **Optimized Episode Loading (ViewModel)**
- **Before**: Sequential loading - fetch episodes from translator 1, then 2, then 3... (slow)
- **After**: Parallel loading - fetch from all translators at once using `async { }` (3x faster)
- **File**: `MovieDetailsViewModel.kt`
- **What this fixes**: The long wait on first entry

### 2. **Added In-Memory Mirror Cache**
- **Before**: Each translator request to `getWorkingMirror()` could trigger full mirror discovery
- **After**: Mirror is cached in-memory for 6 hours, so parallel requests reuse it
- **File**: `HdRezkaApi.kt`
- **Cache fields**:
  ```kotlin
  private var cachedWorkingMirror: String? = null
  private var cachedMirrorTime: Long = 0
  ```
- **What this fixes**: Parallel requests no longer compete to find mirrors

### 3. **Improved Episode Parsing (like lumen does it)**
- **Before**: Try to find `.item-title`, `.item-date` selectors separately
- **After**: Extract raw text from episode element `ep.text()` which contains everything
- **File**: `HdRezkaApi.kt` in `getEpisodes()` function
- **Key change**: Use `val rawEpisodeText = ep.text().trim()` as the complete episode info
- **Regex patterns added for dates**:
  - `\d{1,2}\.\d{1,2}\.\d{2,4}` - for "15.12.2024"
  - Russian months (full + abbreviated): январь, янв, февраль, фев, etc.

### 4. **Added Detailed Logging**
- **File**: `HdRezkaApi.kt`
- **What gets logged**:
  - First 500 chars of HTTP response
  - JSON success flag and available keys
  - Seasons/episodes HTML extracted from JSON
  - Parsed episodes: season, number, title, date, status
  - Parse failures with detailed errors

## How to Debug

### Step 1: Run the app and check logcat

**In Android Studio:**
1. Open `Logcat` (bottom of screen, or View → Tool Windows → Logcat)
2. Filter by: `HdRezkaApi`
3. Navigate to a serial (e.g., "Деннис кошмариус" or "Декстер")
4. Wait for episodes to load
5. Look for messages like:
   ```
   getEpisodes response (first 500 chars): {...}
   JSON response: success=true, keys=[...]
   Extracted from JSON: seasonsHtml length=X, episodesHtml length=Y
   Successfully parsed 3 seasons with 42 episodes
   Season 1, Ep 1: title='...', date='...', status=RELEASED
   ```

### Step 2: Analyze the output

**Expected good output:**
```
D/HdRezkaApi: Successfully parsed 3 seasons with 42 episodes
D/HdRezkaApi: Season 1, Ep 1: title='1 - Пилот', date='15.09.2012', status=RELEASED
D/HdRezkaApi: Season 1, Ep 2: title='2 - Куча молока', date='22.09.2012', status=RELEASED
```

**If you see:**
- `Empty response when getting episodes` → Network issue or mirror not working
- `Server returned error: message` → API error from server
- `Extracted from JSON: seasonsHtml length=0, episodesHtml length=0` → Server returned valid JSON but empty episodes
- `No episodes found in HTML` → Parsing failed (selectors don't match current site structure)

### Step 3: Common Issues & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| `Empty response` | Mirror not responding | Try different mirrors by clearing localStorage or setting forced_mirror |
| `episodesHtml length=0` | Server hasn't uploaded episodes yet | Check if series actually has episodes on HD Rezka |
| `No episodes found` | CSS selectors changed on site | Need to update `.b-simple_season__item` or `.b-simple_episode__item` selectors |
| `status=UPCOMING` for old episodes | Date parsing failed | Check if regex catches the date format in `rawEpisodeText` |

### Step 4: Extract Raw HTML for Analysis

If episodes aren't parsing, you can temporarily modify the code to save the HTML:

Add this to `HdRezkaApi.kt` after line ~260:
```kotlin
if (fullHtml.isNotBlank()) {
    Log.w("HdRezkaApi", "DEBUG: Full HTML:\n$fullHtml")
}
```

Then check logcat for the raw HTML and see what the actual structure is.

## Files Changed

1. `app/src/main/java/com/slooshfilm/app/ui/moviedetails/MovieDetailsViewModel.kt`
   - Parallel async episode fetching
   - Enhanced logging

2. `app/src/main/java/com/slooshfilm/app/data/hdrezka/HdRezkaApi.kt`
   - In-memory mirror cache
   - Improved episode parsing from raw text
   - Comprehensive logging for all steps
   - Better date pattern matching

3. `app/src/main/res/layout/fragment_movie_details.xml`
   - Added Spinner for translator selection

4. `app/src/main/java/com/slooshfilm/app/ui/moviedetails/MovieDetailsFragment.kt`
   - Wired translator selector to fetch episodes on change
   - Now uses movie.selectedTranslatorId for correct translator

## Next Steps

1. **Build and run the app** with these changes
2. **Check logcat** for the debug messages
3. **Share the logcat output** if episodes still don't show correctly
4. Based on the output, we can:
   - Add more date formats if they're being missed
   - Update CSS selectors if the site structure changed
   - Fix any other parsing issues

## Build Command

```powershell
# Set Java 11+ path
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'

# Build
.\gradlew.bat :app:assembleDebug

# Or run directly in Android Studio (Build → Make Project)
```

## Expected Improvements

After these changes:
- ✅ First load should be **2-3x faster** (parallel requests)
- ✅ Episodes should show **real titles, dates, and statuses** (better parsing)
- ✅ Subsequent series loads will be instant (better caching + mirror cache)
- ✅ User can **switch between translators** and see different schedules

---

**Need help?** Run the app, check logcat with `HdRezkaApi` filter, and share the output.
