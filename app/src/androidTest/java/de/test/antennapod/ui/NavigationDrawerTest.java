package de.test.antennapod.ui;

import android.content.Intent;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.runner.AndroidJUnit4;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.core.feed.Feed;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.fragment.DownloadsFragment;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.PlaybackHistoryFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.test.antennapod.EspressoTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static de.test.antennapod.EspressoTestUtils.onDrawerItem;
import static de.test.antennapod.EspressoTestUtils.waitForView;
import static de.test.antennapod.NthMatcher.first;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

/**
 * User interface tests for MainActivity drawer.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationDrawerTest {

    private UITestUtils uiTestUtils;

    @Rule
    public IntentsTestRule<MainActivity> activityRule = new IntentsTestRule<>(MainActivity.class, false, false);

    @Before
    public void setUp() throws IOException {
        uiTestUtils = new UITestUtils(InstrumentationRegistry.getTargetContext());
        uiTestUtils.setup();

        EspressoTestUtils.clearPreferences();
        EspressoTestUtils.makeNotFirstRun();
        EspressoTestUtils.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
        uiTestUtils.tearDown();
    }

    private void openNavDrawer() {
        onView(isRoot()).perform(waitForView(withId(R.id.drawer_layout), 1000));
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
    }

    @Test
    public void testClickNavDrawer() throws Exception {
        uiTestUtils.addLocalFeedData(false);
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        // queue
        openNavDrawer();
        onDrawerItem(withText(R.string.queue_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.recyclerView), 1000));
        assertEquals(activity.getString(R.string.queue_label), activity.getSupportActionBar().getTitle());

        // episodes
        openNavDrawer();
        onDrawerItem(withText(R.string.episodes_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.episodes_label), activity.getSupportActionBar().getTitle());

        // Subscriptions
        openNavDrawer();
        onDrawerItem(withText(R.string.subscriptions_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.subscriptions_grid), 1000));
        assertEquals(activity.getString(R.string.subscriptions_label), activity.getSupportActionBar().getTitle());

        // downloads
        openNavDrawer();
        onDrawerItem(withText(R.string.downloads_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.downloads_label), activity.getSupportActionBar().getTitle());

        // playback history
        openNavDrawer();
        onDrawerItem(withText(R.string.playback_history_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
        assertEquals(activity.getString(R.string.playback_history_label), activity.getSupportActionBar().getTitle());

        // add podcast
        openNavDrawer();
        onView(withId(R.id.nav_list)).perform(swipeUp());
        onDrawerItem(withText(R.string.add_feed_label)).perform(click());
        onView(isRoot()).perform(waitForView(withId(R.id.txtvFeedurl), 1000));
        assertEquals(activity.getString(R.string.add_feed_label), activity.getSupportActionBar().getTitle());

        // podcasts
        for (int i = 0; i < uiTestUtils.hostedFeeds.size(); i++) {
            Feed f = uiTestUtils.hostedFeeds.get(i);
            openNavDrawer();
            onDrawerItem(withText(f.getTitle())).perform(scrollTo(), click());
            onView(isRoot()).perform(waitForView(withId(android.R.id.list), 1000));
            assertEquals("", activity.getSupportActionBar().getTitle());
        }
    }

    @Test
    public void testGoToPreferences() {
        activityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(withText(R.string.settings_label)).perform(click());
        intended(hasComponent(PreferenceActivity.class.getName()));
    }

    @Test
    public void testDrawerPreferencesHideSomeElements() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        activityRule.launchActivity(new Intent());
        openNavDrawer();
        onDrawerItem(withText(R.string.queue_label)).perform(longClick());
        onView(withText(R.string.episodes_label)).perform(click());
        onView(withText(R.string.playback_history_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(EpisodesFragment.TAG));
        assertTrue(hidden.contains(PlaybackHistoryFragment.TAG));
    }

    @Test
    public void testDrawerPreferencesUnhideSomeElements() {
        List<String> hidden = Arrays.asList(PlaybackHistoryFragment.TAG, DownloadsFragment.TAG);
        UserPreferences.setHiddenDrawerItems(hidden);
        activityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(first(withText(R.string.queue_label))).perform(longClick());

        onView(withText(R.string.downloads_label)).perform(click());
        onView(withText(R.string.queue_label)).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(2, hidden.size());
        assertTrue(hidden.contains(QueueFragment.TAG));
        assertTrue(hidden.contains(PlaybackHistoryFragment.TAG));
    }


    @Test
    public void testDrawerPreferencesHideAllElements() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        activityRule.launchActivity(new Intent());
        String[] titles = activityRule.getActivity().getResources().getStringArray(R.array.nav_drawer_titles);

        openNavDrawer();
        onView(first(withText(R.string.queue_label))).perform(longClick());
        for (int i = 0; i < titles.length; i++) {
            String title = titles[i];
            onView(first(withText(title))).perform(click());

            if (i == 3) {
                onView(withId(R.id.select_dialog_listview)).perform(swipeUp());
            }
        }

        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(titles.length, hidden.size());
        for (String tag : MainActivity.NAV_DRAWER_TAGS) {
            assertTrue(hidden.contains(tag));
        }
    }

    @Test
    public void testDrawerPreferencesHideCurrentElement() {
        UserPreferences.setHiddenDrawerItems(new ArrayList<>());
        activityRule.launchActivity(new Intent());
        openNavDrawer();
        onView(withText(R.string.downloads_label)).perform(click());
        openNavDrawer();

        onView(first(withText(R.string.queue_label))).perform(longClick());
        onView(first(withText(R.string.downloads_label))).perform(click());
        onView(withText(R.string.confirm_label)).perform(click());

        List<String> hidden = UserPreferences.getHiddenDrawerItems();
        assertEquals(1, hidden.size());
        assertTrue(hidden.contains(DownloadsFragment.TAG));
    }
}
