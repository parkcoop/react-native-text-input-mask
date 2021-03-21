package com.RNTextInputMask;

import android.text.TextWatcher;
import android.widget.EditText;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.redmadrobot.inputmask.MaskedTextChangedListener;
import com.redmadrobot.inputmask.helper.AffinityCalculationStrategy;
import com.redmadrobot.inputmask.helper.Mask;
import com.redmadrobot.inputmask.model.CaretString;
import com.redmadrobot.inputmask.model.Notation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RNTextInputMaskModule extends ReactContextBaseJavaModule {

    private static final int TEXT_CHANGE_LISTENER_TAG_KEY = 123456789;
    final List<Notation> customNotations = new ArrayList<Notation>();
    ReactApplicationContext reactContext;

    public RNTextInputMaskModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        customNotations.add(new Notation('*', "*", true));

    }

    @Override
    public String getName() {
        return "RNTextInputMask";
    }

    @ReactMethod
    public void mask(final String maskString,
                     final String inputValue,
                     final boolean autocomplete,
                     final Promise promise) {
//      final Mask mask = new Mask(maskString);


      final Mask mask = new Mask(inputValue, customNotations);

        final String input = inputValue;
      final Mask.Result result = mask.apply(
          new CaretString(
              input,
              input.length(),
              new CaretString.CaretGravity.FORWARD(autocomplete)
          )
      );
      final String output = result.getFormattedText().getString();
      promise.resolve(output);
    }

    @ReactMethod
    public void unmask(final String maskString,
                       final String inputValue,
                       final boolean autocomplete,
                       final Promise promise) {
//      final Mask mask = new Mask(maskString);
        final Mask mask = new Mask(inputValue, customNotations);

        final String input = inputValue;
      final Mask.Result result = mask.apply(
          new CaretString(
              input,
              input.length(),
              new CaretString.CaretGravity.FORWARD(autocomplete)
          )
      );
      final String output = result.getExtractedValue();
      promise.resolve(output);
    }

    @ReactMethod
    public void setMask(final int tag, final String mask, final boolean autocomplete, final boolean autoskip) {
        // We need to use prependUIBlock instead of addUIBlock since subsequent UI operations in
        // the queue might be removing the view we're looking to update.
        reactContext.getNativeModule(UIManagerModule.class).prependUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                // The view needs to be resolved before running on the UI thread because there's
                // a delay before the UI queue can pick up the runnable.
                final EditText editText = (EditText) nativeViewHierarchyManager.resolveView(tag);

                reactContext.runOnUiQueueThread(new Runnable() {
                    @Override
                    public void run() {
                        if (editText.getTag(TEXT_CHANGE_LISTENER_TAG_KEY) != null) {
                            editText.removeTextChangedListener((TextWatcher) editText.getTag(TEXT_CHANGE_LISTENER_TAG_KEY));
                        }
                        MaskedTextChangedListener listener = new OnlyChangeIfRequiredMaskedTextChangedListener(mask, autocomplete, autoskip, editText, customNotations);
                        editText.addTextChangedListener(listener);
                        editText.setOnFocusChangeListener(listener);
                        editText.setTag(TEXT_CHANGE_LISTENER_TAG_KEY, listener);
                    }
                });
            }
        });
    }
}

/**
 * Need to extend MaskedTextChangedListener to ignore re-masking previous text (causes weird input behavior in React Native)
 */
class OnlyChangeIfRequiredMaskedTextChangedListener extends MaskedTextChangedListener {
    private String previousText;
    public OnlyChangeIfRequiredMaskedTextChangedListener(@NotNull String primaryFormat, boolean autocomplete, boolean autoskip, @NotNull EditText field, List<Notation> customNotations) {
        super(primaryFormat, Collections.<String>emptyList(), customNotations, AffinityCalculationStrategy.WHOLE_STRING, autocomplete, autoskip, field, null, null, false);
    }

    @Override
    public void beforeTextChanged(@Nullable CharSequence s, int start, int count, int after) {
        previousText = s.toString();
        super.beforeTextChanged(s, start, count, after);
    }

    @Override
    public void onTextChanged(@NotNull final CharSequence s, final int start, final int before, final int count) {
        if (count == 0 && before == 0) {
            return;
        }

        String newText = s.toString().substring(start, start + count);
        String oldText = previousText.substring(start, start + before);

        if (count == before && newText.equals(oldText)) {
            return;
        }
        super.onTextChanged(s, start, before, count);
    }
}
