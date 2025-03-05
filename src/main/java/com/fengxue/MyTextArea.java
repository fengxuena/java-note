package com.fengxue;

import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;

public class MyTextArea extends TextArea {
    @Override
    protected Skin<?> createDefaultSkin() {
        // 返回我们自定义的皮肤实例
        return new MyTextAreaSkin(this);
    }

}
