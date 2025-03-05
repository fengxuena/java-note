package com.fengxue;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.scene.control.skin.TextAreaSkin;
import com.sun.jna.platform.mac.SystemB;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class NoteUI extends Application implements Initializable {
    private boolean ispenetrate=false;
    private boolean sizelock=true;
    private long lastsavetime;
    private String nowlable;//当前标签名称
    private boolean firststar=true;//启动后是否已刷新的标识
    private VBox vBox;
    @FXML private TextArea textvw;
    @FXML private ListView listvw;
    @FXML private VBox vboxs;
    @FXML private Parent root;
    private static Stage primaryStage;
    //窗体拉伸
    private static boolean isBottomRight;// 是否处于右下角调整窗口状态
    private final static int RESIZE_WIDTH = 10;// 判定是否为调整窗口状态的范围与边界距离
    private final static double MIN_WIDTH = 280;// 窗口最小宽度
    private final static double MIN_HEIGHT = 375;// 窗口最小高度
    //窗口位置
    private double xOffset = 0;
    private double yOffset = 0;
    private Map<String,String> datacache;
    private String lastlable;
    private String addlable;
    private boolean delllable=false;

    @Override//入口
    public void start(Stage stage) {
        Map<String, String> config = configget();
        //外窗口
        stage.initStyle(StageStyle.UTILITY);
        stage.setOpacity(0);
        stage.show();
        //内窗口
        primaryStage=new Stage();
        primaryStage.initOwner(stage);
        // 获取主屏幕的宽度和高度
        Screen primaryScreen = Screen.getPrimary();
        double screenWidth = primaryScreen.getVisualBounds().getWidth();
        double screenHeight = primaryScreen.getVisualBounds().getHeight();
        // 设置窗口的尺寸
        primaryStage.setWidth(screenWidth *0.33); // 设置窗口宽度
        primaryStage.setHeight(screenHeight*0.5); // 设置窗口高度
        primaryStage.setX(screenWidth *0.33); // 水平位置
        primaryStage.setY(0); // 垂直位置
        //加载fxml
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/sample.fxml"));
        try { root=loader.load(); } catch (IOException e) { e.printStackTrace(); }
        Scene scen = new Scene(root);
        scen.setFill(Color.rgb(0,0,0,0.0));
        scen.getStylesheets().add("note.css");
        primaryStage.setScene(scen);
        primaryStage.setTitle("windowsnote");
        //primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.initStyle(StageStyle.UNDECORATED);	// 无装饰窗口样式
        primaryStage.initStyle(StageStyle.TRANSPARENT);	// 透明窗口样式
        //获取控制器，设置控件事件
        NoteUI controller = loader.getController();
        vBox = controller.vboxs;
        TextArea textField = controller.textvw;
        ListView listView=controller.listvw;
        vBox.setStyle("-fx-background: rgb(0,0,0,0.0);");
        // 添加list位置鼠标移动窗口事件
        listView.setOnMousePressed(event -> {
            xOffset = primaryStage.getX() - event.getScreenX();
            yOffset = primaryStage.getY() - event.getScreenY(); });
        listView.setOnMouseDragged(event -> {
            if (sizelock){
            primaryStage.setX(event.getScreenX() + xOffset);
            primaryStage.setY(event.getScreenY() + yOffset);
            config.put("横", String.valueOf(event.getScreenX() + xOffset));
            config.put("纵", String.valueOf(event.getScreenY() + yOffset));
            }});
        // 阻止默认的右键菜单显示
        Font.loadFont(getClass().getResourceAsStream("瑞云浓楷书.TTF"),18);
        textField.setPrefHeight(screenHeight*0.5-45);
        textField.setWrapText(true);
        textField.setFont(Font.font("瑞云浓楷书.TTF"));
        textField.setOnContextMenuRequested(event -> {event.consume();});
        //鼠标调整大小和位置
        root.setOnMouseMoved(event -> {
            double x = event.getSceneX();
            double y = event.getSceneY();
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();
            Cursor cursorType = Cursor.DEFAULT;// 鼠标光标初始为默认类型，若未进入调整窗口状态，保持默认类型
            isBottomRight =  false;// 先将所有调整窗口状态重置
            if (y >= height - RESIZE_WIDTH) {
                if (x >= width - RESIZE_WIDTH) {// 右下角调整窗口状态
                    isBottomRight = true;
                    cursorType = Cursor.SE_RESIZE;}}
            root.setCursor(cursorType);});
        root.setOnMouseDragged(event -> {
            if (sizelock){
                double x = event.getSceneX();
                double y = event.getSceneY();
                // 保存窗口改变后的x、y坐标和宽度、高度，用于预判是否会小于最小宽度、最小高度
                double nextWidth = primaryStage.getWidth();
                double nextHeight = primaryStage.getHeight();
                if (isBottomRight) {// 所有右边调整窗口状态
                    nextWidth = x; }
                if (isBottomRight) {// 所有下边调整窗口状态
                    nextHeight = y; }
                if (nextWidth <= MIN_WIDTH) {// 如果窗口改变后的宽度小于最小宽度，则宽度调整到最小宽度
                    nextWidth = MIN_WIDTH; }
                if (nextHeight <= MIN_HEIGHT) {// 如果窗口改变后的高度小于最小高度，则高度调整到最小高度
                    nextHeight = MIN_HEIGHT; }
                // 最后统一改变窗口的x、y坐标和宽度、高度，可以防止刷新频繁出现的屏闪情况
                primaryStage.setWidth(nextWidth);
                primaryStage.setHeight(nextHeight);
                config.put("宽", String.valueOf(nextWidth));
                config.put("高", String.valueOf(nextHeight));
                //textField.setPrefWidth(nextHeight-10);
                textField.setPrefHeight(nextHeight-45);}
            Cursor cursorType = Cursor.DEFAULT;
            root.setCursor(cursorType);
        });
        // 加载配置
        if (config != null) {
            primaryStage.setAlwaysOnTop(Boolean.parseBoolean(config.get("是否置顶")));
            sizelock = Boolean.parseBoolean(config.get("是否锁定"));
            boolean isshow= Boolean.parseBoolean(config.get("是否显示"));
            ispenetrate=Boolean.parseBoolean(config.get("鼠标穿透"));
            if (ispenetrate){ WindowsUtils.setMousePassthrough(true, primaryStage); }
            primaryStage.setX(Double.parseDouble(config.get("横")));
            primaryStage.setY(Double.parseDouble(config.get("纵")));
            primaryStage.setWidth(Double.parseDouble(config.get("宽")));
            primaryStage.setHeight(Double.parseDouble(config.get("高")));
            //textField.setPrefWidth(Double.parseDouble(config.get("宽"))-10);
            textField.setPrefHeight(Double.parseDouble(config.get("高"))-45);
            scen.setFill(Color.rgb(0,0,0,Double.parseDouble(config.get("透明度"))));
            if (isshow){primaryStage.show();}else {primaryStage.show();primaryStage.hide();}
        }
        // 判断是否支持系统托盘
        if (FXTrayIcon.isSupported()) {
            // 创建系统托盘
            FXTrayIcon fxTrayIcon = new FXTrayIcon(primaryStage,  getClass().getClassLoader().getResource("logo.png"));
            // 添加系统托盘菜单
            MenuItem display_window = new MenuItem("显示窗口");
            fxTrayIcon.addMenuItem(display_window);
            MenuItem hide_window = new MenuItem("隐藏窗口");
            fxTrayIcon.addMenuItem(hide_window);
            MenuItem lock_window = new MenuItem("锁定窗口");
            fxTrayIcon.addMenuItem(lock_window);
            MenuItem unlock_window = new MenuItem("解锁窗口");
            fxTrayIcon.addMenuItem(unlock_window);
            MenuItem window_top = new MenuItem("窗口置顶");
            fxTrayIcon.addMenuItem(window_top);
            MenuItem window_down = new MenuItem("窗口置底");
            fxTrayIcon.addMenuItem(window_down);
            MenuItem window_penetrate = new MenuItem("鼠标穿透");
            fxTrayIcon.addMenuItem(window_penetrate);
            MenuItem set_transparency = new MenuItem("设置透明度");
            fxTrayIcon.addMenuItem(set_transparency);
            MenuItem exitItem = new MenuItem("退出&保存");
            fxTrayIcon.addMenuItem(exitItem);
            // 为菜单添加事件处理
            // 显示窗口
            display_window.setOnAction(actionEvent -> { primaryStage.show();config.put("显示窗口","true");});
            //隐藏窗口
            hide_window.setOnAction(actionEvent -> {primaryStage.hide();config.put("显示窗口","false");});
            //锁定窗口
            lock_window.setOnAction(actionEvent -> {sizelock=false;config.put("是否锁定","false");});
            //解锁窗口
            unlock_window.setOnAction(actionEvent -> {sizelock=true;config.put("是否锁定","true");});
            //窗口置顶
            window_top.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(true);
                config.put("是否置顶","true");

            });
            //窗口置底
            window_down.setOnAction(actionEvent -> {
                config.put("是否置顶","false");
                primaryStage.setAlwaysOnTop(false);
                primaryStage.toBack(); // 将窗口置于所有其他窗口之后

            });
            //鼠标穿透
            window_penetrate.setOnAction(actionEvent -> {
                if (ispenetrate){
                    WindowsUtils.setMousePassthrough(false, primaryStage);
                    ispenetrate=false;
                    config.put("鼠标穿透","false");
                }else {
                    WindowsUtils.setMousePassthrough(true, primaryStage);
                    ispenetrate=true;
                    config.put("鼠标穿透","true");
                }
            });
            //设置透明度
            set_transparency.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(false);
                Slider slider = new Slider(0.1, 1.0, primaryStage.getOpacity());
                slider.setMajorTickUnit(0.1);
                slider.setMinorTickCount(0);
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                // 给滑块添加监听器
                slider.valueProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        scen.setFill(Color.rgb(0,0,0,newValue.doubleValue()));
                        config.put("透明度",String.valueOf(newValue));
                        //System.out.println(newValue);
                    }});
                // 创建包含滑块的布局容器
                VBox vBox2 = new VBox(20);
                vBox2.setAlignment(Pos.CENTER);
                vBox2.getChildren().add(slider);
                Dialog<Void> dialog = new Dialog<>();
                dialog.setTitle("设置窗口透明度");
                dialog.setGraphic(null); // 不需要图标
                dialog.getDialogPane().setContent(vBox2);
                // 添加关闭按钮（默认存在，这里只是显式指定）
                dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                // 显示对话框
                dialog.show();


            });
            //退出程序
            exitItem.setOnAction(actionEvent -> {configsave(config); System.exit(0); });
            //单击显示窗口
            fxTrayIcon.setOnAction(actionEvent -> {primaryStage.show();});
            fxTrayIcon.show();// 显示系统托盘
        }else {System.exit(0);// 如果不支持，关闭窗口直接退出程序
        }
    }
    @Override//初始化保持时间
    public void initialize(URL location, ResourceBundle resources) {lastsavetime=System.currentTimeMillis();inits();}
    //初始化数据
    public void inits(){
        ObservableList<String> items= FXCollections.observableArrayList();
        items.clear();
        datacache = dataget();

        // 设置为单选模式
        listvw.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listvw.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                String selectedItem = String.valueOf(listvw.getSelectionModel().getSelectedItem());
                nowlable=selectedItem;
                if (mouseEvent.getButton()== MouseButton.SECONDARY){//鼠标右键
                    if (!selectedItem.equals("添加便签")) {
                        primaryStage.setAlwaysOnTop(false);
                        Dialog<ButtonType> dialogs = new Dialog<>();
                        dialogs.setTitle("是否删除此标签？");
                        dialogs.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
                        dialogs.showAndWait().ifPresent(result -> {
                            if (result == ButtonType.YES) {
                                datacache.remove(nowlable);
                                datasave(datacache);
                                firststar=false;
                                delllable=true;
                                inits();
                            }else {primaryStage.setAlwaysOnTop(true);}});}
                }else {
                if (selectedItem.equals("添加便签")) {//鼠标左键
                    primaryStage.setAlwaysOnTop(false);
                    Dialog<String> dialog = new Dialog<>();
                    dialog.setTitle("请输入便签名称：");
                    ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
                    TextField inputField = new TextField();
                    dialog.getDialogPane().setContent(inputField);
                    dialog.setResultConverter(dialogButton -> {
                        if (dialogButton == okButtonType) { return inputField.getText(); }
                        if (dialogButton==ButtonType.CANCEL){return "cancel";}
                        return null; });
                    dialog.showAndWait().ifPresent(result -> {
                        if (result.equals("cancel")){primaryStage.setAlwaysOnTop(true);}else {
                        datacache.put(String.valueOf(result),"");
                        datasave(datacache);
                        firststar=false;
                        addlable=String.valueOf(result);
                        inits();}});
                }else {
                    if (nowlable.equals("添加便签")){}else {
                        if (lastlable!=null){
                        datacache.put(lastlable,textvw.getText());
                        datasave(datacache);}}
                    lastlable=selectedItem;
                    textvw.setText(datacache.get(nowlable));}}
            }});

        if (firststar){
            listvw.getSelectionModel().selectFirst();
            String selectedItem = String.valueOf(listvw.getSelectionModel().getSelectedItem());
            nowlable=selectedItem;
            if (datacache==null){}else {
            String tex=datacache.get(selectedItem);
            textvw.setText(tex);}
            firststar=false;}else { textvw.setText(datacache.get(nowlable));}
        //失焦时储存
        primaryStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                //System.out.println("失去焦点");
                if (nowlable.equals("添加便签")){}else {
                    String text=textvw.getText();
                    datacache.put(nowlable,text);
                    datasave(datacache);}
            }});
        //不能最小化
        primaryStage.iconifiedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // 如果窗口尝试最小化，则立即恢复它
                Platform.runLater(() -> primaryStage.setIconified(false));
            }
        });
        //加载列表
        if (datacache==null){
            items.add("添加便签");
            listvw.setItems(items);
        }else {
            Set list=datacache.keySet();
            for (Object a:list){items.add(a.toString());}
            items.add("添加便签");
            listvw.setItems(items);
            if (delllable){lastlable=null;delllable=false;}
            if (addlable!=null){listvw.getSelectionModel().select(0);lastlable=null;addlable=null;}
        }
    }
    /**
     * 将数据保存为Base64编码的字符串到文件中。
     */
    public static void datasave(Map<String, String> maps) {
        //System.out.println(maps.toString());
        Gson gson = new Gson();
        JsonElement jieguo = gson.toJsonTree(maps, Map.class);
        // 将JSON字符串转换为Base64编码的字符串
        String base64EncodedData = Base64.getEncoder().encodeToString(jieguo.toString().getBytes(StandardCharsets.UTF_8));
        String fileName = "NoteData.json";
        String localAppData = System.getenv("LOCALAPPDATA") + "\\fengxue\\note";
        File sharedDir = new File(localAppData);
        String savefilepath = sharedDir + "\\" + fileName;
        File file = new File(sharedDir, fileName);
        try (FileWriter writer = new FileWriter(savefilepath)) {
            writer.write(base64EncodedData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 从文件中读取Base64编码的数据并解码回Map对象。
     */
    public static Map<String, String> dataget() {
        Map<String, String> maps = new HashMap();
        String fileName = "NoteData.json";
        String localAppData = System.getenv("LOCALAPPDATA") + "\\fengxue\\note";
        File sharedDir = new File(localAppData);
        if (!sharedDir.exists()) { sharedDir.mkdirs(); }
        String savefilepath = sharedDir + "\\" + fileName;
        File file = new File(sharedDir, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                return maps;
            } catch (IOException e) { e.printStackTrace(); } }
        try (BufferedReader reader = new BufferedReader(new FileReader(savefilepath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            // 解码Base64字符串为原始JSON字符串
            byte[] decodedBytes = Base64.getDecoder().decode(content.toString());
            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String,String> out = gson.fromJson(jsonString, mapType);
            if (out==null){return maps;}
            return out;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //config读取
    public Map configget(){
        String fileName = "Config.json";
        String localAppData = System.getenv("LOCALAPPDATA")+"\\fengxue\\note";//C:\Users\San\AppData\Local
        File sharedDir = new File(localAppData);
        if (sharedDir.exists()){}else {sharedDir.mkdirs();}
        String savefilepath=sharedDir+"\\"+fileName;
        File file = new File(sharedDir, fileName);
        if (file.exists()){}else { try { file.createNewFile();
            Map<String,String> defultmap=new HashMap<>();
            defultmap.put("是否置顶","true");
            defultmap.put("是否锁定","false");
            defultmap.put("是否显示","true");
            defultmap.put("鼠标穿透","false");
            defultmap.put("横","0");
            defultmap.put("纵","0");
            defultmap.put("高","500.0");
            defultmap.put("宽","250.0");
            defultmap.put("透明度","0.7");
            configsave(defultmap);
            return defultmap;
        } catch (IOException e) { e.printStackTrace(); } }
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(savefilepath));
            String content = new String(encoded, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();

            //System.out.println(content); // For debugging purposes

            return gson.fromJson(content, mapType);
        } catch (IOException e) {throw new RuntimeException(e);}
    }
    //config储存
    public void configsave(Map<String,String> map) {
        Gson gson = new Gson();
        JsonElement jieguo = gson.toJsonTree(map, new TypeToken<Map<String, String>>(){}.getType());
        String fileName = "Config.json";
        String localAppData = System.getenv("LOCALAPPDATA") + "\\fengxue\\note";
        File sharedDir = new File(localAppData);
        File file = new File(sharedDir, fileName);
        if (!sharedDir.exists()) { sharedDir.mkdirs(); }
        try {
            if (file.exists() || file.createNewFile()) {
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                    gson.toJson(jieguo, writer);
                } catch (IOException e) { e.printStackTrace(); } }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
/*
    //储存记录
    public static void datasave(Map<String,String> maps){
        Map olddict=dataget();
        if (olddict==null){olddict=maps;}else {olddict.putAll(maps);}
        Gson gson = new Gson();
        JsonElement jieguo= gson.toJsonTree(olddict,Map.class);
        String fileName = "NoteData.json";
        String localAppData = System.getenv("LOCALAPPDATA")+"\\fengxue\\note";//C:\Users\San\AppData\Local
        File sharedDir = new File(localAppData);
        String savefilepath=sharedDir+"\\"+fileName;
        File file = new File(sharedDir, fileName);
        if(file.exists()){
            try {
                FileWriter writer = new FileWriter(savefilepath);
                gson.toJson(jieguo,writer);
                writer.close();
            } catch (IOException e) {throw new RuntimeException(e);}
        }

    }
    //读取记录
    public static Map dataget(){
        String fileName = "NoteData.json";
        String localAppData = System.getenv("LOCALAPPDATA")+"\\fengxue\\note";//C:\Users\%LOCALAPPDATA%\AppData\Local
        File sharedDir = new File(localAppData);
        if (sharedDir.exists()){}else {sharedDir.mkdirs();}
        String savefilepath=sharedDir+"\\"+fileName;
        File file = new File(sharedDir, fileName);
        if (file.exists()){}else { try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } }
        try {
            BufferedReader reader=new BufferedReader(new FileReader(savefilepath));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {content.append(line);}
            reader.close();
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> map = gson.fromJson(content.toString(), mapType);
            return map;
        } catch (IOException e) {throw new RuntimeException(e);}
    }
    //覆盖记录
    public static void datasavefuxie(Map<String,String> maps){
        Gson gson = new Gson();
        JsonElement jieguo= gson.toJsonTree(maps,Map.class);
        String fileName = "NoteData.json";
        String localAppData = System.getenv("LOCALAPPDATA")+"\\fengxue\\note";//C:\Users\San\AppData\Local
        File sharedDir = new File(localAppData);
        String savefilepath=sharedDir+"\\"+fileName;
        File file = new File(sharedDir, fileName);
        file.delete();
        try {file.createNewFile();} catch (IOException e) { e.printStackTrace();}
        if(file.exists()){
            try {
                FileWriter writer = new FileWriter(savefilepath);
                gson.toJson(jieguo,writer);
                writer.close();
            } catch (IOException e) {throw new RuntimeException(e);}
        }
    }*/
/*滚动条
.text-area .scroll-bar:vertical{
        -fx-pref-height: 1;
        -fx-opacity: 0;
        }

        .text-area .scroll-bar {
    -fx-visible: false;
}
* */
