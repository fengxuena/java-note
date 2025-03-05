package com.fengxue;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TextArea;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


public class MyTextAreaSkin extends TextAreaSkin {

    public MyTextAreaSkin(TextArea textArea) {
        super(textArea);
        // 获取 ScrollPane 并设置它的样式以隐藏滚动条
        ScrollPane scrollPane = (ScrollPane) getChildren().get(0);
        if (scrollPane != null) {
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        textArea.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                event.consume(); // 阻止进一步处理此事件
            }
        });
    }
}
