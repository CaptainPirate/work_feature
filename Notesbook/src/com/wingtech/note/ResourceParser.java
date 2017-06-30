/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
 *===================================================================================================*/

package com.wingtech.note;

import android.content.Context;

import com.wingtech.note.R;
import android.util.Log;

public class ResourceParser
{
    public static final int BG_DEFAULT_COLOR = 0;
    public static final int BG_DEFAULT_FONT_SIZE = 1;

    public static int getDefaultBgId(Context context) {
        int i = 0;
        if (PreferenceUtils.getBgAppear(context) > 5)
            i = (int) (Math.random() * NoteBgResources.BG_EDIT_PANEL_RESOURCES.length);
        else
            i = PreferenceUtils.getBgAppear(context);
        return i;
    }

    public static class TextAppearanceResources {
        private static final int[] TEXTAPPEARANCE_RESOURCES = {
                R.style.TextAppearance_Editor_Normal,
                R.style.TextAppearance_Editor_Medium,
                R.style.TextAppearance_Editor_Large,
                R.style.TextAppearance_Editor_Super,

        };
        private static final int[] TEXTAPPEARANCE_RESOURCES_DIALOG = {
            R.style.TextAppearance_Editor_Normal_Dialog,
            R.style.TextAppearance_Editor_Medium_Dialog,
            R.style.TextAppearance_Editor_Large_Dialog,
            R.style.TextAppearance_Editor_Super_Dialog,

        };
        private static final int[] TEXTAPPEARANCE_SIZE_RESOURCES = {
                R.dimen.text_font_size_normal,
                R.dimen.text_font_size_medium,
                R.dimen.text_font_size_large,
                R.dimen.text_font_size_super,
        };

        public static int getTexAppearanceResource(int styleId) {
            if (styleId >= TEXTAPPEARANCE_RESOURCES.length)
                styleId = 1;
            return TEXTAPPEARANCE_RESOURCES[styleId];
        }
        public static int getTexAppearanceResourceDialog(int styleId) {
            if (styleId >= TEXTAPPEARANCE_RESOURCES.length)
                styleId = 1;
            return TEXTAPPEARANCE_RESOURCES_DIALOG[styleId];
        }
        public static int getTexAppearanceSizeResource(int sizeId) {
            if (sizeId >= TEXTAPPEARANCE_SIZE_RESOURCES.length)
                sizeId = 1;
            return TEXTAPPEARANCE_SIZE_RESOURCES[sizeId];
        }

        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;

        }
    }

    public static class WidgetResources {
        private static final int[] BG_RESOURCES = {
                R.drawable.widget_0,
                R.drawable.widget_1,
                R.drawable.widget_2,
                R.drawable.widget_3,
                R.drawable.widget_4,
                R.drawable.widget_5,

        };

        public static int getWidgetBgResource(int paramInt) {
            return BG_RESOURCES[paramInt];
        }
    }

    public static class GridFolderBgResources {
        private static final int[] BG_RESOURCES = {
                R.drawable.grid_folder_0,
                R.drawable.grid_folder_1,
                R.drawable.grid_folder_2,
                R.drawable.grid_folder_3,
                R.drawable.grid_folder_4,
                R.drawable.grid_folder_5,
        };

        public static int getNoteBgRes(int paramInt) {
            return BG_RESOURCES[paramInt];
        }
    }

    public static class GridNoteBgResources {
        private static final int[] BG_RESOURCES = {
                R.drawable.grid_0,
                R.drawable.grid_1,
                R.drawable.grid_2,
                R.drawable.grid_3,
                R.drawable.grid_4,
                R.drawable.grid_5,
        };

        public static int getNoteBgRes(int paramInt) {
            return BG_RESOURCES[paramInt];
        }
    }

    public static class NoteBgResources {
        /* 此处定义主题对应的资源 */
        private static final int[] BG_EDIT_PANEL_RESOURCES = {
                R.drawable.edit_0,
                R.drawable.edit_1,
                R.drawable.edit_2,
                R.drawable.edit_3,
                R.drawable.edit_4,
                R.drawable.edit_5,
        };
        private static final int[] BG_EDIT_TITLE_RESOURCES = {
                R.drawable.edit_title_0,
                R.drawable.edit_title_1,
                R.drawable.edit_title_2,
                R.drawable.edit_title_3,
                R.drawable.edit_title_4,
                R.drawable.edit_title_5,
        };

        private static final int[] BG_EDIT_PANEL_COLOR = {
            R.color.color_bg0,
            R.color.color_bg1,
            R.color.color_bg2,
            R.color.color_bg3,
            R.color.color_bg4,
            R.color.color_bg5,
        };

        public static int getEditPanelBgResource(int paramInt) {
            return BG_EDIT_PANEL_RESOURCES[paramInt];
        }

        public static int getEditTitleBgResource(int paramInt) {
            return BG_EDIT_TITLE_RESOURCES[paramInt];
        }

        public static int getEditPanelBgColor(int paramInt) {
            Log.e("meng","getEditPanelBgColor paramInt="+paramInt);
            return BG_EDIT_PANEL_COLOR[paramInt];
        }
    }

    public static class NoteSRItemBgResources {
        private static final int[] TIME_BG_RESOURCES = {
                R.drawable.list_item_time_bg0,
                R.drawable.list_item_time_bg1,
                R.drawable.list_item_time_bg2,
                R.drawable.list_item_time_bg3,
                R.drawable.list_item_time_bg4,
                R.drawable.list_item_time_bg5,
        };
        private static final int[] Other_BG_RESOURCES = {
            R.drawable.list_item_other_bg0,
            R.drawable.list_item_other_bg1,
            R.drawable.list_item_other_bg2,
            R.drawable.list_item_other_bg3,
            R.drawable.list_item_other_bg4,
            R.drawable.list_item_other_bg5,
    };
        private static final int[] Other_BG = {
                    R.color.color_bg0,
                    R.color.color_bg1,
                    R.color.color_bg2,
                    R.color.color_bg3,
                    R.color.color_bg4,
                    R.color.color_bg5,
            };

        public static int getTimeBgRes(int paramInt) {
            return TIME_BG_RESOURCES[paramInt];
        }
        public static int getOtherBgRes(int paramInt) {
            return Other_BG_RESOURCES[paramInt];
        }

        public static int getOtherBg(int paramInt) {
            return Other_BG[paramInt];
        }
    }

    public static class SketchResources {
        private static final int[] COLOR_RESOURCES = {
                R.color.sketch_pencolor1,
                R.color.sketch_pencolor2,
                R.color.sketch_pencolor3,
                R.color.sketch_pencolor4,
                R.color.sketch_pencolor5,
                R.color.sketch_pencolor6,
                R.color.sketch_pencolor7,
                R.color.sketch_pencolor8,
        };
        private static final int[] WIDTH_RESOURCES = {
                R.dimen.sketch_widthpicker_item1,
                R.dimen.sketch_widthpicker_item2,
                R.dimen.sketch_widthpicker_item3,
                R.dimen.sketch_widthpicker_item4,
                R.dimen.sketch_widthpicker_item5
        };

        public static int getSketchColorResource(int paramInt) {
            return COLOR_RESOURCES[paramInt];
        }

        public static int getSketchColorSize() {
            return COLOR_RESOURCES.length;
        }

        public static int getSketchWidthResource(int paramInt) {
            return WIDTH_RESOURCES[paramInt];
        }

        public static int getSketchWidthSize() {
            return WIDTH_RESOURCES.length;
        }
    }
}
