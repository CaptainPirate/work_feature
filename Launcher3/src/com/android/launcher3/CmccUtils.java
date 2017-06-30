/*
  * Copyright @ 2015 China Mobile Group Device Co.,Ltd.
  * All rights Reserved.
*/

package com.android.launcher3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

public class CmccUtils {
    private static final String TAG = "CmccUtils";

    public static void storeImage(Context context, Bitmap image, String fileName) {
        storeImage(context, image, fileName, Bitmap.CompressFormat.PNG);
    }

    public static void storeImage(Context context, Bitmap image,
            String fileName, Bitmap.CompressFormat format) {
        if (!Bitmap.CompressFormat.PNG.equals(format)
                && !Bitmap.CompressFormat.JPEG.equals(format)) {
            Log.w(TAG, "storeImage only support jpeg/png format.");
            return;
        }

        File pictureFile = new File(context.getFilesDir() + "/" + fileName);
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(format, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("storeImage", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("storeImage", "Error accessing file: " + e.getMessage());
        }
    }

    public static void getRealScreenSize(Context context, Point size) {
        int x, y, orientation = context.getResources().getConfiguration().orientation;
        WindowManager wm = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point screenSize = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealSize(screenSize);
                x = screenSize.x;
                y = screenSize.y;
            } else {
                display.getSize(screenSize);
                x = screenSize.x;
                y = screenSize.y;
            }
        } else {
            x = display.getWidth();
            y = display.getHeight();
        }

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            size.set(x, y);
        } else {
            size.set(y, x);
        }
    }

    public static Bitmap createScreenShort(Context context, Rect sourceCrop) {
        Point size = new Point();
        Bitmap result = createScreenShort(context, size);
        if (result != null && sourceCrop != null) {
            Bitmap cropped = Bitmap.createBitmap(result, sourceCrop.left, sourceCrop.top,
                    size.x - sourceCrop.left - sourceCrop.right,
                    size.y - sourceCrop.top - sourceCrop.bottom);
            if (cropped != result) {
                result.recycle();
            }
            result = cropped;
        }
        return result;
    }

    private static Bitmap createScreenShort(Context context, Point size) {
        getRealScreenSize(context, size);

        Bitmap result = null;
        try {
            Class clazz = Class.forName("android.view.SurfaceControl");
            Method screenshotMethod = clazz.getMethod("screenshot",
                    new Class[] {int.class, int.class});
            result = (Bitmap) screenshotMethod.invoke(null, size.x, size.y);
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "ex: " + e);
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "ex: " + e);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "ex: " + e);
        } catch (InvocationTargetException e) {
            Log.d(TAG, "ex: " + e);
        }
        return result;
    }

    public boolean isClassHasStaticMethod(String className, String methodName) {
        return getClassStaticMethod(className, methodName) != null;
    }

    public static Method getClassStaticMethod(String className, String methodName) {
        Method defMethod = null;
        try {
            Class clazz = Class.forName(className);
            defMethod = clazz.getMethod(methodName);
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return defMethod;
    }

    public static Object getClassInstance(String className) {
        try {
            Class clazz = Class.forName(className);
            return clazz.newInstance();
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (InstantiationException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    public static Method getClassMethod(Class clazz, String methodName, Class<?>... parameterTypes) {
        Method defMethod = null;
        if (clazz == null) {
            return defMethod;
        }

        try {
            defMethod = clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return defMethod;
    }

    public static Method getClassMethod(String className, String methodName, Class<?>... parameterTypes) {
        Method defMethod = null;
        try {
            Class clazz = Class.forName(className);
            defMethod = clazz.getMethod(methodName, parameterTypes);
        } catch (ClassNotFoundException e) {
            // ignore
        } catch (NoSuchMethodException e) {
            // ignore
        }
        return defMethod;
    }

    public static void shakeViewRotation(final View view, final int numOfShakes, int duration, float degree) {
        if (view == null) {
            return;
        }

        final int w = view.getMeasuredWidth();
        final int h = view.getMeasuredHeight();

        long singleShakeDuration = duration / numOfShakes / 4;
        if (singleShakeDuration == 0) {
            singleShakeDuration = 16;
        }

        final AnimatorSet shakeAnim = new AnimatorSet();
        shakeAnim.playSequentially(
                ObjectAnimator.ofFloat(view, View.ROTATION, degree),
                ObjectAnimator.ofFloat(view, View.ROTATION, 0),
                ObjectAnimator.ofFloat(view, View.ROTATION, -degree),
                ObjectAnimator.ofFloat(view, View.ROTATION, 0));
        shakeAnim.setInterpolator(null);
        shakeAnim.setDuration(singleShakeDuration);

        shakeAnim.addListener(new AnimatorListenerAdapter() {
            private int mShakeCount = 0;
            @Override
            public void onAnimationStart(Animator animation) {
                if (mShakeCount == 0) {
                    view.setPivotY(h * 0.9f);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mShakeCount++;
                if (mShakeCount == numOfShakes) {
                    mShakeCount = 0;
                    view.setPivotY(h / 2);
                } else {
                    shakeAnim.start();
                }
            }
        });
        shakeAnim.start();
    }

    public static void shakeViewtranslationX(View view, final int numOfShakes, int duration, float shakeDistance) {
        if (view == null) {
            return;
        }

        long singleShakeDuration = duration / numOfShakes / 4;
        if (singleShakeDuration == 0) {
            singleShakeDuration = 16;
        }

        final AnimatorSet shakeAnim = new AnimatorSet();
        shakeAnim.playSequentially(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, shakeDistance),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, -shakeDistance),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0));
        shakeAnim.setInterpolator(null);
        shakeAnim.setDuration(singleShakeDuration);

        shakeAnim.addListener(new AnimatorListenerAdapter() {
            private int mShakeCount = 0;
            @Override
            public void onAnimationEnd(Animator animation) {
                mShakeCount++;
                if (mShakeCount == numOfShakes) {
                    mShakeCount = 0;
                } else {
                    shakeAnim.start();
                }
            }
        });
        shakeAnim.start();
    }

    public static void expandStatusBar(Context context) {
        Object service = context.getSystemService("statusbar");
        Class<?> statusbarManager = null;
        try {
            statusbarManager = Class.forName("android.app.StatusBarManager");
        } catch (ClassNotFoundException e) {
            // Ignore
        }

        if (statusbarManager == null) {
            Log.w(TAG, "Can not get StatusBarManager service.");
            return;
        }

        Method expandPanels = null;
        try {
            expandPanels = statusbarManager.getMethod("expandNotificationsPanel");
            expandPanels.invoke(service);
        } catch (NoSuchMethodException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (IllegalArgumentException e) {
            // Ignore
        } catch (InvocationTargetException e) {
            // Ignore
        }
    }
}
