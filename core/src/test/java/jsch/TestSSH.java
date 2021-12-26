package jsch;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import javax.swing.*;
import java.awt.*;

public class TestSSH {
  public static void main(String[] args) throws JSchException {
    final var jSch = new JSch();
    final var session = jSch.getSession("root", "dev.medipath.com.cn");
    session.setUserInfo(new MyUserInfo());
    session.connect(30000);
    final var channel = session.openChannel("shell");
    channel.setInputStream(System.in);
    channel.setOutputStream(System.out);
    channel.connect(3 * 1000);
  }

  public static class MyUserInfo implements UserInfo, UIKeyboardInteractive {
    private String passwd;
    private JTextField passwordField = new JPasswordField(20);
    private Container panel;
    final GridBagConstraints gbc =
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            1,
            1,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0);

    @Override
    public String getPassphrase() {
      return null;
    }

    @Override
    public String getPassword() {
      return passwd;
    }

    @Override
    public boolean promptPassword(String message) {
      final Object[] ob = {passwordField};
      int result = JOptionPane.showConfirmDialog(null, ob, message, JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        passwd = passwordField.getText();
        return true;
      } else {
        return false;
      }
    }

    @Override
    public boolean promptPassphrase(String message) {
      return true;
    }

    @Override
    public boolean promptYesNo(String message) {
      //      return true;
      final Object[] options = {"yes", "no"};
      final var foo =
          JOptionPane.showOptionDialog(
              null,
              message,
              "Warning",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.WARNING_MESSAGE,
              null,
              options,
              options[0]);
      return foo == 0;
    }

    @Override
    public void showMessage(String message) {
      JOptionPane.showMessageDialog(null, message);
    }

    @Override
    public String[] promptKeyboardInteractive(
        String destination, String name, String instruction, String[] prompt, boolean[] echo) {
      panel = new JPanel();
      panel.setLayout(new GridBagLayout());

      gbc.weightx = 1.0;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx = 0;
      panel.add(new JLabel(instruction), gbc);
      gbc.gridy++;

      gbc.gridwidth = GridBagConstraints.RELATIVE;

      JTextField[] texts = new JTextField[prompt.length];
      for (int i = 0; i < prompt.length; i++) {
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 1;
        panel.add(new JLabel(prompt[i]), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        if (echo[i]) {
          texts[i] = new JTextField(20);
        } else {
          texts[i] = new JPasswordField(20);
        }
        panel.add(texts[i], gbc);
        gbc.gridy++;
      }

      if (JOptionPane.showConfirmDialog(
              null,
              panel,
              destination + ": " + name,
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE)
          == JOptionPane.OK_OPTION) {
        String[] response = new String[prompt.length];
        for (int i = 0; i < prompt.length; i++) {
          response[i] = texts[i].getText();
        }
        return response;
      } else {
        return null; // cancel
      }
//      return new String[0];
    }
  }
}
