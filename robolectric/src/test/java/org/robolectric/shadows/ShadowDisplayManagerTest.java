package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.P;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.robolectric.shadows.ShadowDisplayManagerTest.HideFromJB.getGlobal;

import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
public class ShadowDisplayManagerTest {

  private DisplayManager instance;

  @Before
  public void setUp() throws Exception {
    instance = (DisplayManager) RuntimeEnvironment.application
        .getSystemService(Context.DISPLAY_SERVICE);
  }

  @Test @Config(maxSdk = JELLY_BEAN)
  public void notSupportedInJellyBean() throws Exception {
    try {
      ShadowDisplayManager.removeDisplay(0);
      fail("Expected Exception thrown");
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageThat().contains("displays not supported in Jelly Bean");
    }
  }

  @Test
  @Config(minSdk = JELLY_BEAN_MR1)
  public void getDisplayInfo_shouldReturnCopy() throws Exception {
    DisplayInfo displayInfo = getGlobal().getDisplayInfo(Display.DEFAULT_DISPLAY);
    int origAppWidth = displayInfo.appWidth;
    displayInfo.appWidth++;
    assertThat(getGlobal().getDisplayInfo(Display.DEFAULT_DISPLAY).appWidth)
        .isEqualTo(origAppWidth);
  }

  @Test
  @Config(minSdk = JELLY_BEAN_MR1)
  public void forNonexistentDisplay_getDisplayInfo_shouldReturnNull() throws Exception {
    assertThat(getGlobal().getDisplayInfo(3)).isEqualTo(null);
  }

  @Test
  @Config(minSdk = JELLY_BEAN_MR1)
  public void forNonexistentDisplay_changeDisplay_shouldThrow() throws Exception {
    try {
      ShadowDisplayManager.changeDisplay(3, "");
      fail("Expected Exception thrown");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().contains("no display 3");
    }
  }

  @Test
  @Config(minSdk = JELLY_BEAN_MR1)
  public void forNonexistentDisplay_removeDisplay_shouldThrow() throws Exception {
    try {
      ShadowDisplayManager.removeDisplay(3);
      fail("Expected Exception thrown");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageThat().contains("no display 3");
    }
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void addDisplay() throws Exception {
    int displayId = ShadowDisplayManager.addDisplay("w100dp-h200dp");
    assertThat(displayId).isGreaterThan(0);

    DisplayInfo di = getGlobal().getDisplayInfo(displayId);
    assertThat(di.appWidth).isEqualTo(100);
    assertThat(di.appHeight).isEqualTo(200);

    Display display = instance.getDisplay(displayId);
    assertThat(display.getDisplayId()).isEqualTo(displayId);
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void addDisplay_shouldNotifyListeners() throws Exception {
    List<String> events = new ArrayList<>();
    instance.registerDisplayListener(new MyDisplayListener(events), null);
    int displayId = ShadowDisplayManager.addDisplay("w100dp-h200dp");
    assertThat(events).containsExactly("Added " + displayId);
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void changeDisplay_shouldUpdateSmallestAndLargestNominalWidthAndHeight() throws Exception {
    Point smallest = new Point();
    Point largest = new Point();

    ShadowDisplay.getDefaultDisplay().getCurrentSizeRange(smallest, largest);
    assertThat(smallest).isEqualTo(new Point(320, 320));
    assertThat(largest).isEqualTo(new Point(470, 470));

    Display display = ShadowDisplay.getDefaultDisplay();
    ShadowDisplay shadowDisplay = Shadow.extract(display);
    shadowDisplay.setWidth(display.getWidth() - 10);
    shadowDisplay.setHeight(display.getHeight() - 10);

    ShadowDisplay.getDefaultDisplay().getCurrentSizeRange(smallest, largest);
    assertThat(smallest).isEqualTo(new Point(310, 310));
    assertThat(largest).isEqualTo(new Point(460, 460));
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void withQualifiers_changeDisplay_shouldUpdateSmallestAndLargestNominalWidthAndHeight() throws Exception {
    Point smallest = new Point();
    Point largest = new Point();

    Display display = ShadowDisplay.getDefaultDisplay();
    display.getCurrentSizeRange(smallest, largest);
    assertThat(smallest).isEqualTo(new Point(320, 320));
    assertThat(largest).isEqualTo(new Point(470, 470));

    ShadowDisplayManager.changeDisplay(display.getDisplayId(), "w310dp-h460dp");

    display.getCurrentSizeRange(smallest, largest);
    assertThat(smallest).isEqualTo(new Point(310, 310));
    assertThat(largest).isEqualTo(new Point(460, 460));
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void changeAndRemoveDisplay_shouldNotifyListeners() throws Exception {
    List<String> events = new ArrayList<>();
    instance.registerDisplayListener(new MyDisplayListener(events), null);
    int displayId = ShadowDisplayManager.addDisplay("w100dp-h200dp");

    ShadowDisplayManager.changeDisplay(displayId, "w300dp-h400dp");

    Display display = getGlobal().getRealDisplay(displayId);
    assertThat(display.getWidth()).isEqualTo(300);
    assertThat(display.getHeight()).isEqualTo(400);
    assertThat(display.getOrientation()).isEqualTo(Surface.ROTATION_0);

    ShadowDisplayManager.removeDisplay(displayId);

    assertThat(events).containsExactly(
        "Added " + displayId,
        "Changed " + displayId,
        "Removed " + displayId);
  }

  @Test @Config(minSdk = JELLY_BEAN_MR1)
  public void changeDisplay_shouldAllowPartialChanges() throws Exception {
    List<String> events = new ArrayList<>();
    instance.registerDisplayListener(new MyDisplayListener(events), null);
    int displayId = ShadowDisplayManager.addDisplay("w100dp-h200dp");

    ShadowDisplayManager.changeDisplay(displayId, "+h201dp-land");

    Display display = getGlobal().getRealDisplay(displayId);
    assertThat(display.getWidth()).isEqualTo(201);
    assertThat(display.getHeight()).isEqualTo(100);
    assertThat(display.getOrientation()).isEqualTo(Surface.ROTATION_90);

    assertThat(events).containsExactly(
        "Added " + displayId,
        "Changed " + displayId);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_defaultValue_shouldReturnOne() {
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(1.0f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToZero_shouldReturnZero() {
    instance.setSaturationLevel(0.0f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(0.0f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToHalf_shouldReturnHalf() {
    instance.setSaturationLevel(0.5f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(0.5f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToOne_shouldReturnOne() {
    instance.setSaturationLevel(0.0f);
    instance.setSaturationLevel(1.0f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(1.0f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToZeroViaShadow_shouldReturnZero() {
    ShadowDisplayManager.setSaturationLevel(0.0f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(0.0f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToHalfViaShadow_shouldReturnHalf() {
    ShadowDisplayManager.setSaturationLevel(0.5f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(0.5f);
  }

  @Test
  @Config(minSdk = P)
  public void getSaturationLevel_setToOneViaShadow_shouldReturnOne() {
    ShadowDisplayManager.setSaturationLevel(0.0f);
    ShadowDisplayManager.setSaturationLevel(1.0f);
    assertThat(ShadowDisplayManager.getSaturationLevel()).isEqualTo(1.0f);
  }

  @Test @Config(minSdk = P)
  public void setSaturationLevel_setToValueGreaterThanOne_shouldThrow() {
    try {
      instance.setSaturationLevel(1.1f);
      fail("Expected IllegalArgumentException thrown");
    } catch (IllegalArgumentException expected) {}
  }

  @Test @Config(minSdk = P)
  public void setSaturationLevel_setToNegativevalue_shouldThrow() {
    try {
      instance.setSaturationLevel(-0.1f);
      fail("Expected IllegalArgumentException thrown");
    } catch (IllegalArgumentException expected) {}
  }

  @Test @Config(minSdk = P)
  public void setSaturationLevel_setToValueGreaterThanOneViaShadow_shouldThrow() {
    try {
      ShadowDisplayManager.setSaturationLevel(1.1f);
      fail("Expected IllegalArgumentException thrown");
    } catch (IllegalArgumentException expected) {}
  }

  @Test @Config(minSdk = P)
  public void setSaturationLevel_setToNegativevalueViaShadow_shouldThrow() {
    try {
      ShadowDisplayManager.setSaturationLevel(-0.1f);
      fail("Expected IllegalArgumentException thrown");
    } catch (IllegalArgumentException expected) {}
  }

  // because DisplayInfo and DisplayManagerGlobal don't exist in Jelly Bean,
  // and we don't want them resolved as part of the test class.
  static class HideFromJB {
    static DisplayInfo createDisplayInfo(int width, int height) {
      DisplayInfo displayInfo = new DisplayInfo();
      displayInfo.appWidth = width;
      displayInfo.appHeight = height;
      return displayInfo;
    }

    public static DisplayManagerGlobal getGlobal() {
      return DisplayManagerGlobal.getInstance();
    }
  }

  private static class MyDisplayListener implements DisplayManager.DisplayListener {
    private final List<String> events;

    MyDisplayListener(List<String> events) {
      this.events = events;
    }

    @Override
    public void onDisplayAdded(int displayId) {
      events.add("Added " + displayId);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
      events.add("Removed " + displayId);
    }

    @Override
    public void onDisplayChanged(int displayId) {
      events.add("Changed " + displayId);
    }
  }
}
