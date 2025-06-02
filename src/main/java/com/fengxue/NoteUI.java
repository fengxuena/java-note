package com.fengxue;
import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.melloware.jintellitype.JIntellitype;
import com.sun.jna.CallbackReference;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class NoteUI extends Application implements Initializable{
    private boolean ispenetrate=false;
    private boolean sizelock=false;
    private String nowlable;//当前标签名称
    private volatile boolean need_choose_first=true;//启动后是否已刷新的标识
    @FXML private TextArea textarea;
    @FXML private ListView listview;
    @FXML private VBox vboxs;
    @FXML private Parent root;
    @FXML private HBox hboxs;
    @FXML private Button addbt;
    @FXML private Label lastSaveLabel;
    private static Stage primaryStage;
    //窗体拉伸
    private final static int RESIZE_WIDTH = 10;// 判定是否为调整窗口状态的范围与边界距离
    private final static double MIN_WIDTH = 280;// 窗口最小宽度
    private final static double MIN_HEIGHT = 375;// 窗口最小高度
    //窗口位置
    private double xOffset = 0;
    private double yOffset = 0;
    private SqlHelper sqlHelper=new SqlHelper();
    private boolean delllable=false;
    private  int HOTKEY_ID = 1001;
    private Timeline saveTimeline;
    private double screenHeights;
    private static final String USER_CSS_PATH = System.getenv("LOCALAPPDATA") + "\\fengxue\\note\\note.css";
    private static final String RESOURCE_CSS_PATH = "/note.css"; // resources 下的路径
    private static final String USER_IMAGE_PATH = System.getenv("LOCALAPPDATA") + "\\fengxue\\note\\addbotton.png";
    private static final String RESOURCE_IMAGE_PATH = "/addbotton.png";
    private double savedWidth;
    private double savedHeight;
    private double savedx;
    private double savedy;
    private double savetransparency;

    public static void main(String[] args) { launch(NoteUI.class, args); }
    @Override//入口
    public void start(Stage stage) throws IOException {
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
        screenHeights=screenHeight;
        // 设置窗口的尺寸
        primaryStage.setWidth(screenWidth *0.33); // 设置窗口宽度
        primaryStage.setHeight(screenHeight*0.5); // 设置窗口高度
        primaryStage.setX(screenWidth *0.33); // 水平位置
        primaryStage.setY(0); // 垂直位置
        //加载fxml：等待initialize执行完成
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/sample.fxml"));
        root=loader.load();
        //设置样式
        NoteUI controller = loader.getController();
        textarea=controller.textarea;
        listview=controller.listview;
        vboxs=controller.vboxs;
        hboxs=controller.hboxs;
        addbt=controller.addbt;
        lastSaveLabel=controller.lastSaveLabel;
        Scene scene = new Scene(root);
        ensureUserCSSExists();
        ensureUserImageExists();
        scene.setFill(Color.rgb(0,0,0,0.0));
        scene.getStylesheets().add(new File(USER_CSS_PATH).toURI().toString());
        primaryStage.setScene(scene);
        primaryStage.setTitle("windowsnote");
        //primaryStage.initStyle(StageStyle.UTILITY);
        //primaryStage.initStyle(StageStyle.UNDECORATED);	// 无装饰窗口样式
        primaryStage.initStyle(StageStyle.TRANSPARENT);	// 透明窗口样式
        //鼠标调整大小和位置
        root.setOnMouseMoved(event -> {
            double x = event.getSceneX();
            double y = event.getSceneY();
            double width = primaryStage.getWidth();
            double height = primaryStage.getHeight();

            Cursor cursorType = Cursor.DEFAULT;

            boolean atLeft = x <= RESIZE_WIDTH;
            boolean atRight = x >= width - RESIZE_WIDTH;
            boolean atBottom = y >= height - RESIZE_WIDTH;

            if (atLeft || atRight || atBottom) {
                if (atLeft && atBottom) {
                    cursorType = Cursor.SW_RESIZE;
                } else if (atRight && atBottom) {
                    cursorType = Cursor.SE_RESIZE;
                } else if (atLeft) {
                    cursorType = Cursor.W_RESIZE;
                } else if (atRight) {
                    cursorType = Cursor.E_RESIZE;
                } else if (atBottom) {
                    cursorType = Cursor.S_RESIZE;
                }

            }

            root.setCursor(cursorType);
        });
        root.setOnMouseDragged(event -> {
            if (sizelock) return;

            double x = event.getSceneX();
            double y = event.getSceneY();
            double nextWidth = primaryStage.getWidth();
            double nextHeight = primaryStage.getHeight();
            double newX = primaryStage.getX();
            double newY = primaryStage.getY();

            Cursor cursor = root.getCursor();

            // 处理左右边拖动（E_RESIZE, SE_RESIZE, SW_RESIZE）
            if (cursor == Cursor.E_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
                nextWidth = x;
            } else if (cursor == Cursor.W_RESIZE) {
                // 左边缘拖动：保持右边不动，改变左边位置和宽度
                newX = event.getScreenX();
                nextWidth = primaryStage.getX() + primaryStage.getWidth() - newX;
            }

            // 处理下边拖动（S_RESIZE, SE_RESIZE, SW_RESIZE）
            if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
                nextHeight = y;
            }

            // 应用最小尺寸限制
            if (nextWidth < MIN_WIDTH) nextWidth = MIN_WIDTH;
            if (nextHeight < MIN_HEIGHT) nextHeight = MIN_HEIGHT;

            // 更新窗口尺寸和位置
            primaryStage.setWidth(nextWidth);
            primaryStage.setHeight(nextHeight);
            primaryStage.setX(newX);
            primaryStage.setY(newY);

            // 更新文本区域高度
            if (textarea != null) {
                textarea.setPrefHeight(nextHeight - 45); // 减去固定偏移量
            }
            savedWidth=nextWidth;
            savedHeight=nextHeight;


        });
        root.setOnMouseReleased(event -> {
            if (savedHeight!=0 &&  savedWidth!=0){
                // 保存配置
                sqlHelper.update("config", "宽", String.valueOf(savedWidth));
                sqlHelper.update("config", "高", String.valueOf(savedHeight));
                savedHeight=0;
                savedWidth=0;
            }

        });
        //阻止最小化
        preventMinimizeOnShowDesktop();
        // 加载配置
        load_config(scene);
        // 创建系统托盘
        createTrayIcon(scene);
        //失焦时储存
        primaryStage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String text= textarea.getText();
                if (nowlable!=null){
                    sqlHelper.update("lable",  nowlable, text);
                    updateLastSaveTime(false);
                }

            }});
        // 鼠标进入 textvw 时恢复默认光标
        textarea.setOnMouseMoved(event -> {
            if (root.getCursor() != Cursor.DEFAULT) {
                root.setCursor(Cursor.DEFAULT); }
        });
        //透明背景
        vboxs.setStyle("-fx-background: rgb(0,0,0,0.0);");
        // 添加list位置鼠标移动窗口事件
        listview.setOnMousePressed(event -> {
            xOffset = primaryStage.getX() - event.getScreenX();
            yOffset = primaryStage.getY() - event.getScreenY(); });
        listview.setOnMouseDragged(event -> {
            if (!sizelock){
                primaryStage.setX(event.getScreenX() + xOffset);
                primaryStage.setY(event.getScreenY() + yOffset);
                //移动完成后写入数据库
                savedx=event.getScreenX() + xOffset;
                savedy=event.getScreenY() + yOffset;
            }});
        listview.setOnMouseReleased(event -> {
            if (savedx!=0 && savedy!=0){
                // 保存配置
                sqlHelper.update("config", "横", String.valueOf(savedx));
                sqlHelper.update("config", "纵", String.valueOf(savedy));
                savedx=0;
                savedy=0;
            }});
        // 阻止默认的右键菜单显示
        Font.loadFont(getClass().getResourceAsStream("狮尾黑体.TTF"),18);
        textarea.setPrefHeight(screenHeights*0.5-45);
        textarea.setWrapText(true);
        textarea.setFont(Font.font("狮尾黑体.TTF"));
        textarea.setOnContextMenuRequested(event -> {event.consume();});
        listview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listview.setOnMouseClicked(mouseEvent -> {
            String selectedItem = String.valueOf(listview.getSelectionModel().getSelectedItem());
            if (mouseEvent.getButton()== MouseButton.PRIMARY){//鼠标左键
                if (nowlable!=null){
                    sqlHelper.update("lable",  nowlable, textarea.getText()); }
                if (selectedItem!=null){
                    List<Map<String, String>> maps = sqlHelper.queryForList("lable", selectedItem);
                    if (maps!=null && maps.size()>0){
                        String text=maps.get(0).get("myvalue");
                        textarea.setText(text); }
                    nowlable=selectedItem;
                }
                initdata();
            }else if (mouseEvent.getButton()== MouseButton.SECONDARY){//鼠标右键
                primaryStage.setAlwaysOnTop(false);
                Dialog<ButtonType> dialogs = new Dialog<>();
                dialogs.setTitle("是否删除便签:<"+selectedItem+">？");
                dialogs.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
                dialogs.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.YES) {
                        sqlHelper.delete("lable", selectedItem);
                        need_choose_first=true;
                        primaryStage.setAlwaysOnTop(true);
                    }else {primaryStage.setAlwaysOnTop(true);}});
                initdata();
            }
        });
        initdata();

    }
    @Override//在FXMLLoader执行后执行它
    public void initialize(URL location, ResourceBundle resources) {
        //initdata();
        setupAutoSave();

    }
    //初始化数据
    public void initdata(){
        //System.out.println("lableget-----------------------------------------");
        ObservableList<String> items= FXCollections.observableArrayList();
        Map<String, String> data = sqlHelper.lableget();
        if (data!=null && data.size()>0){
            listview.getItems().removeAll();
            for (String s : data.keySet()){ items.add(s); }
            listview.setItems(items);

            if (need_choose_first){
                synchronized (this){
                    //System.out.println("need_choose_first触发");
                    listview.getSelectionModel().selectFirst();
                    need_choose_first=false;
                }
            }
            nowlable = String.valueOf(listview.getSelectionModel().getSelectedItem());
            //System.out.println("nowlable:"+nowlable);
            try {
                String text=data.get(nowlable);
                textarea.setText(text);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //托盘菜单
    private void createTrayIcon(Scene scene) {
        if (FXTrayIcon.isSupported()) {
            // 创建系统托盘
            FXTrayIcon fxTrayIcon = new FXTrayIcon(primaryStage,  getClass().getClassLoader().getResource("logo.png"));
            //System.out.println("当前字符集：" + java.nio.charset.Charset.defaultCharset());
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
            MenuItem set_auto_save = new MenuItem("自动保存时间");
            fxTrayIcon.addMenuItem(set_auto_save);
            MenuItem set_hotkey = new MenuItem("鼠标穿透热键");
            fxTrayIcon.addMenuItem(set_hotkey);
            MenuItem set_theme = new MenuItem("设置CSS");
            fxTrayIcon.addMenuItem(set_theme);
            MenuItem set_transparency = new MenuItem("设置整体透明度");
            fxTrayIcon.addMenuItem(set_transparency);
            MenuItem exitItem = new MenuItem("退出");
            fxTrayIcon.addMenuItem(exitItem);
            // 显示窗口
            display_window.setOnAction(actionEvent -> { primaryStage.show();sqlHelper.update("config", "显示窗口", "1");});
            //隐藏窗口
            hide_window.setOnAction(actionEvent -> {primaryStage.hide();sqlHelper.update("config", "显示窗口", "0");});
            //锁定窗口
            lock_window.setOnAction(actionEvent -> {sizelock=true;sqlHelper.update("config", "是否锁定","1");});
            //解锁窗口
            unlock_window.setOnAction(actionEvent -> {sizelock=false;sqlHelper.update("config", "是否锁定","0");});
            //窗口置顶
            window_top.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(true);
                sqlHelper.update("config", "是否置顶","1");
            });
            //窗口置底
            window_down.setOnAction(actionEvent -> {
                sqlHelper.update("config", "是否置顶","0");
                primaryStage.setAlwaysOnTop(false);
                primaryStage.toBack(); // 将窗口置于所有其他窗口之后

            });
            //鼠标穿透
            window_penetrate.setOnAction(actionEvent -> {
                if (ispenetrate){
                    setMousePassthrough(false, primaryStage);
                    ispenetrate=false;
                    sqlHelper.update("config", "鼠标穿透","0");
                }else {
                    setMousePassthrough(true, primaryStage);
                    ispenetrate=true;
                    sqlHelper.update("config", "鼠标穿透","1");
                }
            });
            //设置自动保存时间
            set_auto_save.setOnAction(actionEvent -> showAutoSaveSettingsDialog());
            //设置热键
            set_hotkey.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(false);
                Map<String, String> config=sqlHelper.configget();
                String hotkeyMain="Alt";
                String hotkeySub="Q";
                if (config!=null && config.size()>0){
                    hotkeyMain=config.get("热键主");
                    hotkeySub =config.get("热键副");
                }
                Dialog<Boolean> dialog = new Dialog<>();
                VBox dialogVBox = new VBox(10);
                Label titleLabel = new Label("注：设置后重启软件生效！");
                Label mainLabel = new Label("选择主热键：");
                ToggleGroup mainGroup = new ToggleGroup();
                RadioButton altRadio = new RadioButton("Alt");
                RadioButton ctrlRadio = new RadioButton("Ctrl");
                altRadio.setToggleGroup(mainGroup);
                ctrlRadio.setToggleGroup(mainGroup);
                Label subLabel = new Label("输入副热键（单个字母）：");
                TextField subKeyField = new TextField();
                if (hotkeyMain!=null && hotkeyMain.equals("Alt")){
                    altRadio.setSelected(true);
                }else if (hotkeyMain!=null && hotkeyMain.equals("Ctrl")){
                    ctrlRadio.setSelected(true);
                }else {
                    altRadio.setSelected(true);
                }
                if (hotkeySub != null && !hotkeySub.isEmpty()){
                    subKeyField.setText(hotkeySub);
                }else {
                    subKeyField.setText("Q");
                }
                Button closeButton = new Button("关闭");
                closeButton.setOnAction(e -> {
                    dialog.setResult(Boolean.TRUE);
                    dialog.close();
                    primaryStage.setAlwaysOnTop(true);
                });
                Button confirmButton = new Button("设置");
                confirmButton.setDefaultButton(true);
                confirmButton.setOnAction(e -> {
                    String selectedMain = ((RadioButton) mainGroup.getSelectedToggle()).getText();
                    String subKeyStr = subKeyField.getText().trim().toUpperCase();
                    if (subKeyStr.length() == 1 && Character.isLetter(subKeyStr.charAt(0))) {
                        char subKeyChar = subKeyStr.charAt(0);
                        sqlHelper.update("config", "热键主", selectedMain);
                        sqlHelper.update("config", "热键副", subKeyStr);
                        dialog.setResult(Boolean.TRUE);
                        dialog.close();
                        primaryStage.setAlwaysOnTop(true);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING, "请输入一个有效的字母作为副热键。");
                        alert.showAndWait();
                    }
                });
                dialogVBox.getChildren().addAll(titleLabel,mainLabel, altRadio, ctrlRadio, subLabel, subKeyField, confirmButton,closeButton);
                dialog.setTitle("设置全局热键-重启生效");
                dialog.getDialogPane().setContent(dialogVBox);
                dialog.initOwner(primaryStage);
                dialog.show();
            });
            //设置CSS
            set_theme.setOnAction(actionEvent -> showThemeSettingsDialog(scene));
            //设置外窗口透明度
            set_transparency.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(false);
                Dialog<Boolean> dialog = new Dialog<>();
                Slider slider = new Slider(0.01, 1.0, primaryStage.getOpacity());
                slider.setMajorTickUnit(0.25);
                slider.setMinorTickCount(0);
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                // 给滑块添加监听器
                slider.valueProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                        scene.setFill(Color.rgb(0,0,0,newValue.doubleValue()));
                        savetransparency=newValue.doubleValue();
                    }});
                Button closeButton = new Button("关闭");
                closeButton.setOnAction(e -> {
                    sqlHelper.update("config", "透明度",String.valueOf(savetransparency));
                    dialog.setResult(Boolean.TRUE);
                    dialog.close();
                    primaryStage.setAlwaysOnTop(true);
                });
                // 创建包含滑块的布局容器
                VBox vBox2 = new VBox(20, slider, closeButton);
                vBox2.setAlignment(Pos.CENTER);
                dialog.setTitle("设置窗口透明度");
                dialog.setGraphic(null); // 不需要图标
                dialog.getDialogPane().setContent(vBox2);
                dialog.initOwner(primaryStage);
                dialog.show();
            });
            //退出程序
            exitItem.setOnAction(actionEvent -> {
                sqlHelper.close();
                if (saveTimeline!=null){saveTimeline.stop();}
                System.exit(0);
            });
            //单击显示窗口
            fxTrayIcon.setOnAction(actionEvent -> {
                primaryStage.setAlwaysOnTop(true);
                sqlHelper.update("config", "是否置顶", "1");
                primaryStage.show();});
            fxTrayIcon.show();// 显示系统托盘
        }else {System.exit(0); }// 如果不支持，关闭窗口直接退出程序
    }
    // 加载配置
    private void load_config(Scene scene){
        if (sqlHelper != null) {
            Map<String, String> config = sqlHelper.configget();
            if (config != null && config.size() > 0){
                primaryStage.setAlwaysOnTop(Boolean.parseBoolean(config.get("是否置顶")));
                sizelock = Boolean.parseBoolean(config.get("是否锁定"));
                boolean isshow= Boolean.parseBoolean(config.get("是否显示"));
                ispenetrate=Boolean.parseBoolean(config.get("鼠标穿透"));
                primaryStage.setX(Double.parseDouble(config.get("横")));
                primaryStage.setY(Double.parseDouble(config.get("纵")));
                primaryStage.setWidth(Double.parseDouble(config.get("宽")));
                primaryStage.setHeight(Double.parseDouble(config.get("高")));
                textarea.setPrefHeight(Double.parseDouble(config.get("高"))-45);
                //if (textarea==null){System.out.println("textarea为空");}
                //if (config==null){System.out.println("config为空");}
                scene.setFill(Color.rgb(0,0,0,Double.parseDouble(config.get("透明度"))));
                if (isshow){primaryStage.show();}else {primaryStage.show();primaryStage.hide();}
                //注册快捷键和窗口不可最小化
                int modifiers=config.get("热键主").equals("Ctrl") ? JIntellitype.MOD_CONTROL  : JIntellitype.MOD_ALT;
                String hotkeySub = config.get("热键副");
                char subKeyChar = 'Q';
                if (hotkeySub != null && !hotkeySub.isEmpty() ) { subKeyChar = hotkeySub.charAt(0); }
                registerHotKey(HOTKEY_ID,modifiers, subKeyChar);
            }

        }
    }
    //阻止窗口最小化
    private void preventMinimizeOnShowDesktop() {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, primaryStage.getTitle());
        if (hwnd != null) {
            // 使用 GetWindowLong 替代 GetWindowLongPtr
            long oldProcAddr = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_WNDPROC);
            final WinUser.WindowProc oldProc = new WinUser.WindowProc() {
                @Override
                public WinDef.LRESULT callback(WinDef.HWND hWnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
                    if (uMsg == WinUser.WM_SYSCOMMAND && wParam.longValue() == WinUser.SC_MINIMIZE) {
                        return new WinDef.LRESULT(0); // 阻止最小化
                    }
                    return User32.INSTANCE.CallWindowProc(new Pointer(oldProcAddr), hWnd, uMsg, wParam, lParam);
                }
            };
            // 获取 WindowProc 回调的 native 函数指针地址
            Pointer windowProcPtr = CallbackReference.getFunctionPointer(oldProc);
            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_WNDPROC, windowProcPtr.getInt(0));
        }
    }
    //热键
    private void registerHotKey(int ID,int modifiers,  char subKeyChar) {
        if (!JIntellitype.getInstance().isJIntellitypeSupported()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "不支持 JIntellitype。");
            alert.show();
            return; }
        JIntellitype.getInstance().registerHotKey(ID, modifiers, subKeyChar);
        JIntellitype.getInstance().addHotKeyListener(hotKeyId -> {
            if (hotKeyId == ID) {
                ispenetrate = !ispenetrate;
                setMousePassthrough(ispenetrate, primaryStage);
            }
        });

    }
    @FXML//添加按钮
    private void onAddLabelClicked() {
        primaryStage.setAlwaysOnTop(false);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("请输入便签名称：");
        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        TextField inputField = new TextField();
        dialog.getDialogPane().setContent(inputField);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return inputField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            //.println("Result: " + result);
            if (result != null && !result.isEmpty()) {
                sqlHelper.insert("lable", result, "");
                need_choose_first=true;
                initdata();
            }
            primaryStage.setAlwaysOnTop(true);
        });
    }
    //鼠标穿透功能
    public static void setMousePassthrough(boolean enable, Stage stage) {
        // 使用窗口标题查找 HWND
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
        if (hwnd != null && !hwnd.equals(new WinDef.HWND())) {
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, User32.GWL_EXSTYLE);
            if (enable) {
                exStyle |= User32.WS_EX_TRANSPARENT;
            } else {
                exStyle &= ~User32.WS_EX_TRANSPARENT;
            }
            User32.INSTANCE.SetWindowLong(hwnd, User32.GWL_EXSTYLE, exStyle);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING, "无法找到窗口句柄,无法设置鼠标穿透");
            alert.showAndWait();
        }
    }
    //主题设置
    private void showThemeSettingsDialog(Scene scene) {
        primaryStage.setAlwaysOnTop(false);
        Dialog<Boolean> themeSettingsDialog = new Dialog<>();
        themeSettingsDialog.setTitle("主题设置");
        Button openCssFileBtn = new Button("打开CSS文件");
        openCssFileBtn.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(USER_CSS_PATH));
            } catch (IOException ex) { ex.printStackTrace(); }
        });
        Button reloadBtn = new Button("应用CSS样式");
        reloadBtn.setOnAction(e -> {
            reloadStylesheet(scene);
        });
        Button resetBtn = new Button("恢复默认CSS");
        resetBtn.setOnAction(e -> {
            File userCSSFile = new File(USER_CSS_PATH);
            if (userCSSFile.exists()){
                userCSSFile.delete();
                ensureUserCSSExists();
            }
        });
        Button closeBtn = new Button("关闭");
        closeBtn.setOnAction(e -> {
            themeSettingsDialog.setResult(Boolean.TRUE);
            themeSettingsDialog.close();
            primaryStage.setAlwaysOnTop(true);
        });
        VBox dialogContent = new VBox(10, openCssFileBtn, reloadBtn, resetBtn,closeBtn);
        dialogContent.setAlignment(Pos.CENTER);
        themeSettingsDialog.getDialogPane().setContent(dialogContent);
        //themeSettingsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        themeSettingsDialog.initOwner(primaryStage);
        themeSettingsDialog.show();
    }
    //应用css
    public void reloadStylesheet(Scene scene) {
        String css = new File(USER_CSS_PATH).toURI().toString();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(css);
    }
    //颜色选择器
    private void showColorPickerDialog(TextField colorField) {
        ColorPicker colorPicker = new ColorPicker();
        try {
            colorPicker.setValue(Color.web(colorField.getText()));
        } catch (Exception e) {
            colorPicker.setValue(Color.BLACK); // 默认黑色
        }

        Dialog<Color> dialog = new Dialog<>();
        dialog.setTitle("选择颜色");
        dialog.setHeaderText("请选择颜色");
        dialog.getDialogPane().setContent(colorPicker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return colorPicker.getValue();
            }
            return null;
        });

        Optional<Color> result = dialog.showAndWait();
        result.ifPresent(color -> {
            String hex = toRGBHex(color);
            colorField.setText(hex);
        });
    }
    //颜色转Hex
    public static String toRGBHex(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
    //创建用户CSS文件
    private void ensureUserCSSExists() {
        File userCSSFile = new File(USER_CSS_PATH);

        if (userCSSFile.exists()) return;

        // 创建父目录
        userCSSFile.getParentFile().mkdirs();

        try (InputStream is = getClass().getResourceAsStream(RESOURCE_CSS_PATH);
             OutputStream os = new FileOutputStream(userCSSFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //创建添加按钮图片
    private void ensureUserImageExists() {
        File userImageFile = new File(USER_IMAGE_PATH);
        if (userImageFile.exists()) return;

        // 创建父目录
        userImageFile.getParentFile().mkdirs();

        try (InputStream is = getClass().getResourceAsStream(RESOURCE_IMAGE_PATH);
             OutputStream os = new FileOutputStream(userImageFile)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 初始化定时器
    private void setupAutoSave() {
        Map<String, String> config = sqlHelper.configget();
        int second=Integer.parseInt(String.valueOf(config.get("自动保存时间")));
        if (second == 0) {return;}
        saveTimeline = new Timeline(new KeyFrame(Duration.seconds(second), event -> {
            if (nowlable != null && textarea != null) {
                String currentText = textarea.getText();
                sqlHelper.update("config", nowlable,currentText);
                updateLastSaveTime(true);
            }
        }));
        saveTimeline.setCycleCount(Animation.INDEFINITE); // 无限循环
        saveTimeline.play(); // 启动定时器
    }
    //更新保存时间
    private void updateLastSaveTime(boolean isaotu) {
        String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        if (isaotu){
            lastSaveLabel.setText("上次自动保存时间：" + currentTime);
        }else {
            lastSaveLabel.setText("上次手动保存时间：" + currentTime);
        }
    }
    //自动保存设置
    private void showAutoSaveSettingsDialog() {
        primaryStage.setAlwaysOnTop(false);
        Map<String, String> config = sqlHelper.configget();
        String second=String.valueOf(config.get("自动保存时间"));
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("设置自动保存时间");
        dialog.setHeaderText("请输入自动保存间隔时间（秒）\n范围：5 - 300 秒，0 表示关闭自动保存");
        // 输入框
        TextField secondsField = new TextField();
        //secondsField.setText(String.valueOf(saveTimeline != null ? (int) saveTimeline.getKeyFrames().get(0).getTime().toSeconds() : 60));
        // 确定按钮
        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(secondsField);

        secondsField.setText(second);
        // 转换结果
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                try {
                    int seconds = Integer.parseInt(secondsField.getText());
                    if (seconds < 0 || seconds > 300) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("输入错误");
                        alert.setHeaderText(null);
                        alert.setContentText("请输入 0 到 300 之间的数值");
                        alert.showAndWait();
                        return null;
                    }

                    return seconds;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("输入错误");
                    alert.setHeaderText(null);
                    alert.setContentText("请输入有效的整数");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();

        result.ifPresent(seconds -> {
            primaryStage.setAlwaysOnTop(true);
            sqlHelper.update("config", "自动保存时间",String.valueOf(seconds));
            if (saveTimeline != null) { saveTimeline.stop(); }// 停止旧的定时器
            if (seconds == 0) { return; }
            // 启动新的定时器
            saveTimeline = new Timeline(new KeyFrame(Duration.seconds(seconds), event -> {
                System.out.println(System.currentTimeMillis()+"自动保存"+nowlable+" "+textarea.getText());
                if (nowlable != null && textarea != null) {
                    String currentText = textarea.getText();
                    sqlHelper.update("config", nowlable,currentText);
                    updateLastSaveTime(true); }
            }));
            saveTimeline.setCycleCount(Animation.INDEFINITE);
            saveTimeline.play();
        });
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
