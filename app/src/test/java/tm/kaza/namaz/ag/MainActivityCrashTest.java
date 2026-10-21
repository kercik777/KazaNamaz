package tm.kaza.namaz.ag;

import android.app.Activity;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import tm.kaza.namaz.ag.data.BackupManager;
import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.ui.DayDetailSheet;

/**
 * Прогоняет запуск и ключевые сценарии приложения без эмулятора.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityCrashTest {

    @Test
    public void fullStartupAndAllTabsDoNotCrash() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create()
                .start()
                .resume()
                .visible()
                .get();
        Assert.assertNotNull(activity);

        BottomNavigationView nav = activity.findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_calendar);
        nav.setSelectedItemId(R.id.nav_stats);
        nav.setSelectedItemId(R.id.nav_settings);
        nav.setSelectedItemId(R.id.nav_main);
    }

    @Test
    public void togglePrayerAndQuickActionsDoNotCrash() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        // Строки намазов созданы (6 штук)
        LinearLayout container = activity.findViewById(R.id.prayerContainer);
        Assert.assertNotNull(container);
        Assert.assertEquals(6, container.getChildCount());

        // Отмечаем первый намаз (Фаджр) как пропущенный
        View firstRow = container.getChildAt(0);
        View btnMissed = firstRow.findViewById(R.id.btnMissed);
        Assert.assertNotNull(btnMissed);
        btnMissed.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Отмечаем его же обратно как выполненный
        firstRow = container.getChildAt(0);
        View btnDone = firstRow.findViewById(R.id.btnDone);
        Assert.assertNotNull(btnDone);
        btnDone.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // «Всё выполнено»
        activity.findViewById(R.id.btnAllDone).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // «Сегодня»
        activity.findViewById(R.id.btnToday).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void dayDetailSheetDoesNotCrash() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        Calendar c = Calendar.getInstance();
        DayDetailSheet.show(activity, DataStore.get(activity), c, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void backupExportImportRoundTripWorks() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        DataStore ds = DataStore.get(activity);
        String key = DataStore.dateKey(Calendar.getInstance());
        ds.setMissed(key, 0, true);
        ds.setNote(key, "тестовая заметка");

        String json = BackupManager.exportJson(ds);
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("\"KazaNamaz\""));
        Assert.assertTrue(json.contains(key));

        BackupManager.ImportResult res = BackupManager.parseImport(json);
        Assert.assertTrue(res.valid);
        Assert.assertTrue(res.records.containsKey(key));
        Assert.assertEquals(1, res.records.get(key)[0]);
        Assert.assertEquals("тестовая заметка", res.notes.get(key));

        ds.replaceAll(res.records, res.notes);
        Assert.assertEquals(1, ds.countMissed(key));
        Assert.assertEquals("тестовая заметка", ds.getNote(key));

        ds.clearAll();
        Assert.assertEquals(0, ds.totalMissed());
    }

    @Test
    public void markingOnePrayerMissedKeepsOthersDone() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        DataStore ds = DataStore.get(activity);
        ds.clearAll();

        // Отмечаем только Фаджр как пропущенный
        LinearLayout container = activity.findViewById(R.id.prayerContainer);
        View firstRow = container.getChildAt(0);
        firstRow.findViewById(R.id.btnMissed).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        String key = DataStore.dateKey(Calendar.getInstance());
        // Пропущен только один намаз (индекс 0 — Фаджр)
        Assert.assertEquals(1, ds.countMissed(key));
        int[] flags = ds.getFlags(key);
        Assert.assertEquals(1, flags[0]);
        Assert.assertEquals(0, flags[1]);
        Assert.assertEquals(0, flags[2]);
        Assert.assertEquals(0, flags[3]);
        Assert.assertEquals(0, flags[4]);
        Assert.assertEquals(0, flags[5]);
    }

    @Test
    public void headerDebtUpdatesAfterToggle() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        DataStore ds = DataStore.get(activity);
        ds.clearAll();

        TextView debt = activity.findViewById(R.id.tvHeaderDebt);
        Assert.assertEquals("0", debt.getText().toString());

        // Переключаем Фаджр в «пропущен» — общий счётчик в шапке должен стать 1
        LinearLayout container = activity.findViewById(R.id.prayerContainer);
        View firstRow = container.getChildAt(0);
        firstRow.findViewById(R.id.btnMissed).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals("1", debt.getText().toString());
    }

    @Test
    public void counterScreenUpdatesIndependentPrayerAndImsakValues() {
        Activity activity = Robolectric.buildActivity(CounterActivity.class)
                .create().start().resume().visible().get();
        DataStore ds = DataStore.get(activity);
        ds.clearAll();

        LinearLayout prayers = activity.findViewById(R.id.counterPrayerContainer);
        LinearLayout other = activity.findViewById(R.id.counterOtherContainer);
        Assert.assertEquals(6, prayers.getChildCount());
        Assert.assertEquals(1, other.getChildCount());

        prayers.getChildAt(0).findViewById(R.id.btnCounterPlus).performClick();
        other.getChildAt(0).findViewById(R.id.btnCounterPlus).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(1, ds.getCounterCounts()[0]);
        Assert.assertEquals(1, ds.getCounterCounts()[6]);

        prayers.getChildAt(0).findViewById(R.id.btnCounterMinus).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(0, ds.getCounterCounts()[0]);
        Assert.assertEquals(1, ds.getCounterCounts()[6]);
    }

    @Test
    public void counterDataIsIndependentAndNeverGoesBelowZero() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();
        DataStore ds = DataStore.get(activity);
        ds.clearAll();
        String key = DataStore.dateKey(Calendar.getInstance());

        ds.setCounterCount(0, 3);
        ds.setStatus(key, 0, 1);
        ds.setCounterCount(0, ds.getCounterCounts()[0] - 1);
        ds.setCounterCount(6, -8);

        Assert.assertEquals(2, ds.getCounterCounts()[0]);
        Assert.assertEquals(0, ds.getCounterCounts()[6]);
        Assert.assertEquals(1, ds.countMissed(key));
        Assert.assertEquals(2, ds.totalCounterDebt());
    }

    @Test
    public void fullBackupRestoresBothModesAndSettings() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();
        DataStore ds = DataStore.get(activity);
        ds.clearAll();
        String key = DataStore.dateKey(Calendar.getInstance());
        ds.setStatus(key, 2, 2);
        ds.setNote(key, "заметка без потери");
        ds.setCounterCounts(new int[]{1, 2, 3, 4, 5, 6, 7});
        ds.setThemeMode(DataStore.THEME_DARK);
        ds.setAppMode(DataStore.MODE_COUNTER);
        ds.setCalculatorProfile("{\"gender\":\"female\",\"madhhab\":1}");

        String json = BackupManager.exportJson(ds);
        BackupManager.ImportResult result = BackupManager.parseImport(json);
        Assert.assertTrue(result.valid);
        Assert.assertTrue(result.settingsIncluded);

        ds.clearAll();
        ds.setThemeMode(DataStore.THEME_AUTO);
        ds.setAppMode(DataStore.MODE_EXTENDED);
        ds.restoreBackup(result.records, result.notes, result.counterCounts,
                result.themeMode, result.appMode, result.calculatorProfile, result.settingsIncluded);

        Assert.assertEquals(2, ds.getFlags(key)[2]);
        Assert.assertEquals("заметка без потери", ds.getNote(key));
        Assert.assertEquals(7, ds.getCounterCounts()[6]);
        Assert.assertEquals(DataStore.THEME_DARK, ds.getThemeMode());
        Assert.assertEquals(DataStore.MODE_COUNTER, ds.getAppMode());
        Assert.assertEquals("{\"gender\":\"female\",\"madhhab\":1}",
                ds.getCalculatorProfile());
    }

    @Test
    public void freshDayIsNeutralByDefault() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        DataStore ds = DataStore.get(activity);
        ds.clearAll();
        String key = DataStore.dateKey(Calendar.getInstance());

        // По умолчанию никаких отметок: ни выполнено, ни пропущено
        int[] flags = ds.getFlags(key);
        for (int f : flags) {
            Assert.assertEquals(0, f);
        }
        Assert.assertEquals(0, ds.countMissed(key));
        Assert.assertEquals(0, ds.countDone(key));
    }

    @Test
    public void togglingPillsReturnsToNeutral() {
        Activity activity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        DataStore ds = DataStore.get(activity);
        ds.clearAll();
        String key = DataStore.dateKey(Calendar.getInstance());

        LinearLayout container = activity.findViewById(R.id.prayerContainer);

        // «Пропущен»: нейтрально -> пропущен
        container.getChildAt(0).findViewById(R.id.btnMissed).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(1, ds.countMissed(key));
        Assert.assertEquals(1, ds.getFlags(key)[0]);

        // Повторный клик «Пропущен»: пропущен -> нейтрально (отмена)
        container.getChildAt(0).findViewById(R.id.btnMissed).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(0, ds.countMissed(key));
        Assert.assertEquals(0, ds.getFlags(key)[0]);

        // «Выполнен»: нейтрально -> выполнен
        container.getChildAt(0).findViewById(R.id.btnDone).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(2, ds.getFlags(key)[0]);

        // Повторный клик «Выполнен»: выполнен -> нейтрально (отмена)
        container.getChildAt(0).findViewById(R.id.btnDone).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        Assert.assertEquals(0, ds.getFlags(key)[0]);
    }
}
