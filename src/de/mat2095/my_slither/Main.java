package de.mat2095.my_slither;

import javax.swing.UIManager;

public final class Main {

    public static void main(String[] args) throws Exception {

        System.setProperty("sun.java2d.opengl", "false");

        UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");

        new MySlitherJFrame().setVisible(true);

    }
}
