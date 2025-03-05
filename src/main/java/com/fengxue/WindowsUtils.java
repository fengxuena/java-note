package com.fengxue;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import javafx.stage.Stage;

public class WindowsUtils {
    /**
     * 设置窗口的鼠标穿透属性。
     *
     * @param enable  是否启用鼠标穿透
     * @param stage   JavaFX Stage 对象
     */
    public static void setMousePassthrough(boolean enable, Stage stage) {
        // 使用窗口标题查找 HWND
        HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
        if (hwnd != null && !hwnd.equals(new HWND())) {
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, User32.GWL_EXSTYLE);
            if (enable) {
                exStyle |= User32.WS_EX_TRANSPARENT;
            } else {
                exStyle &= ~User32.WS_EX_TRANSPARENT;
            }
            User32.INSTANCE.SetWindowLong(hwnd, User32.GWL_EXSTYLE, exStyle);
        } else {
            System.out.println("无法找到窗口句柄");
        }
    }
}