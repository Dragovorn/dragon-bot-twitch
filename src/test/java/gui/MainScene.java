/*
 * Copyright (c) 2017. Andrew Burr
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction,
 *  including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 *  PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 *  THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package gui;

import com.dragovorn.dragonbot.bot.Version;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class MainScene implements Initializable {

    private static final String connectText = "Join Channel";
    private static final String optionsText = "Options";

    @FXML private Label versionLabel;

    @FXML private TextField channel;

    @FXML private Button connect;
    @FXML private Button options;

    @Override
    public void initialize(URL url, ResourceBundle bundle) {
        this.versionLabel.setText(Version.getPrettyVersion());
        this.channel.lengthProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() > oldValue.intValue()) {
                if (!channel.getText().matches("[a-zA-Z0-9]{0,25}")) {
                    channel.setText(channel.getText().substring(0, channel.getText().length() - 1));
                }
            }
        });
        this.connect.setText(connectText);
        this.options.setText(optionsText);
        this.options.setOnAction(event -> App.getInstance().set("options"));
        this.connect.setOnAction(event -> {
            if (channel.getText().matches("[a-zA-Z0-9]{4,25}")) {
                System.out.println("Proper Twitch Name");
            } else {
                System.out.println("Improper Twitch Name");
            }
        });
    }
}