package org.iseclab.drammer;

import android.app.Instrumentation;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.PerformException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import org.iseclab.drammer.ui.MainActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class EspressoTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final String TAG = EspressoTest.class.getSimpleName();

    private DrammerIdlingResource mServiceStart = null;
    private DrammerIdlingResource mServiceStop = null;

    private static int STALL_TIMEOUT_IN_S = Preferences.ESPRESSO_STALL_TIMEOUT;
    private static int WAIT_TIMEOUT_IN_S = Preferences.ESPRESSO_WAIT_TIMEOUT;
    private static int ESPRESSO_TIMEOUT_IN_S = Preferences.ESPRESSO_TIMEOUT;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    public EspressoTest() {
        super(MainActivity.class);
    }

    private Context getContext() {
        return mActivityRule.getActivity();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "SETUP DRAMMER ESPRESSO TEST");
    }


    @Test
    public void testEspresso(){
        Log.d(TAG, "STARTING DRAMMER ESPRESSO TEST");
        try {
            onView(withId(R.id.button_start)).perform(click());
            Log.d(TAG, "CLICKED START BUTTON");
        } catch (PerformException e){
            Log.d(TAG, "START BUTTON NOT AVAILABLE");
            e.printStackTrace();
            stallTest();
            return;
        }

        stallTest();

        IdlingPolicies.setMasterPolicyTimeout(ESPRESSO_TIMEOUT_IN_S, TimeUnit.SECONDS);
        IdlingPolicies.setIdlingResourceTimeout(ESPRESSO_TIMEOUT_IN_S, TimeUnit.SECONDS);

        String binary = mActivityRule.getActivity().getDrammerBinary();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        /*
        mServiceStart = new DrammerIdlingResource(instrumentation.getTargetContext());
        mServiceStart.setResourceName(binary);
        mServiceStart.setOnStart(true);
        Espresso.registerIdlingResources(mServiceStart);

        Log.d(TAG, "IDLING RESOURCE RETURNED ON DRAMMER START (" + binary + ")");

        stallTest();
        */

        mServiceStop = new DrammerIdlingResource(instrumentation.getTargetContext());
        mServiceStop.setResourceName(binary);
        mServiceStop.setOnStart(false);
        Espresso.registerIdlingResources(mServiceStop);
        Log.d(TAG, "IDLING RESOURCE RETURNED ON DRAMMER STOP (" + binary + ")");

        /* Idling Resource seems to return immediately, still wait until drammer stopped */
        boolean drammerIsRunning = true;
        while(drammerIsRunning){
            Log.d(TAG, "Drammer still running (" + binary + ")...");
            waitForTests();
            drammerIsRunning = mServiceStop.isHammerRunning();
        }

        Log.d(TAG, "FINISHING DRAMMER ESPRESSO TEST");
    }

    private void stallTest(){
        try {
            Thread.sleep(STALL_TIMEOUT_IN_S * 1000);
        } catch (InterruptedException ignored){}
    }

    private void waitForTests(){
        try {
            Thread.sleep(WAIT_TIMEOUT_IN_S * 1000);
        } catch (InterruptedException ignored){}
    }

    @Before
    public void runBeforeTest() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        if(mServiceStart != null){
            Espresso.unregisterIdlingResources(mServiceStart);
        }
        if(mServiceStop != null) {
            Espresso.unregisterIdlingResources(mServiceStop);
        }
        super.tearDown();
    }


}

