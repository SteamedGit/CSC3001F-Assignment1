
import java.io.IOError;
import java.io.IOException;
//import java.nio.file.Paths;
import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.awt.event.*;








public class ClientApp extends JFrame{
    
    private JPanel newPanel ;
    private JPanel panel1 ;
    private JTextField textField2, textField1;
    private JPanel panelimage ;

    private String output = "";

    private String[] contactList = {};

    private JComboBox contactBox = new JComboBox();

    private JTextPane tp = new JTextPane();
    private JScrollPane sp = new JScrollPane(tp);



    public ClientApp(){
        super("ClientApp");


        tp.setEditable(false);
        setLocation(440, 50);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        newPanel = new JPanel();
        panel1 = new JPanel();
        panelimage = new JPanel();

        panel1.setBackground(Color.BLACK);
        panel1.setPreferredSize( new Dimension(500, 500));
        newPanel.setPreferredSize( new Dimension(480, 480));
        panelimage.setPreferredSize( new Dimension(350, 300));
        sp.setPreferredSize(new Dimension(300, 350));

        add(panel1);

        panel1.add(newPanel, BorderLayout.SOUTH);

        newPanel.add(contactBox, BorderLayout.WEST);



        final ActionListener listener = new ButtonListen();
        final JButton buttonA = new JButton("Send Message");


        textField2 = new JTextField(10);
        textField2.setText("Message here");
        Font font = new Font("SansSerif", Font.BOLD, 25);
        textField2.setFont(font);

        newPanel.add(textField2, BorderLayout.CENTER);

        newPanel.add(buttonA, BorderLayout.EAST);
        newPanel.add(sp, BorderLayout.SOUTH);



        pack();
        setVisible(true);
    }


    public static void main(String[] args) {
        Client x = new Client();
        new ClientApp();
    }


    public class ButtonListen implements ActionListener {
        public  void actionPerformed(ActionEvent e) {
            String buttonText = ((JButton)e.getSource()).getText();
            ((JButton)e.getSource()).setEnabled(true);
            String letterType = buttonText;
            try {
                if (letterType.equals("Send Message")){

                    System.out.println("sending message");
                }
            }
            finally {
                System.out.println("final");
            }

        }
    }


}
