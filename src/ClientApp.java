
import java.io.IOError;
import java.io.IOException;
//import java.nio.file.Paths;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.*;








public class ClientApp extends JFrame{

    private String chat;

    private static Client client;
    
    private JPanel newPanel ;
    private JPanel panel1 ;
    private JTextField textField2;
    private JPanel panelimage;
    


    public String[] listOfClients;
    JList<String> myList = new JList<String>();
    

    private JTextPane tp = new JTextPane();
    private JScrollPane sp = new JScrollPane(tp);



    public ClientApp() throws IOException, InterruptedException{
        super("ClientApp");

        
        tp.setEditable(false);
        setLocation(440, 50);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        newPanel = new JPanel();
        panel1 = new JPanel();
        panelimage = new JPanel();

        panel1.setBackground(Color.BLACK);
        panel1.setPreferredSize( new Dimension(600, 800));
        newPanel.setPreferredSize( new Dimension(580, 780));
        panelimage.setPreferredSize( new Dimension(350, 550));
        sp.setPreferredSize(new Dimension(300, 350));



        add(panel1);

        panel1.add(newPanel, BorderLayout.SOUTH);

        this.listOfClients = client.getOtherClients().split(", ");

        final ListSelectionListener  listListener = new ListListen();

        myList  = new JList<String>(listOfClients);
        myList.addListSelectionListener(listListener);

        newPanel.add(myList, BorderLayout.WEST);


        final ActionListener listener = new ButtonListen();
        final JButton buttonA = new JButton("Send Message");
        buttonA.addActionListener(listener);

        final JButton buttonClientRefresh = new JButton("Refresh Client List");
        buttonClientRefresh.addActionListener(listener);

        final JButton buttonRecieve = new JButton("Get Messages");
        buttonRecieve.addActionListener(listener);

        textField2 = new JTextField(10);
        textField2.setText("Message here");
        Font font = new Font("SansSerif", Font.BOLD, 25);
        textField2.setFont(font);

        newPanel.add(textField2, BorderLayout.CENTER);
        
        newPanel.add(buttonA, BorderLayout.EAST);
        newPanel.add(buttonClientRefresh, BorderLayout.WEST);
        newPanel.add(buttonRecieve, BorderLayout.WEST);



        newPanel.add(sp, BorderLayout.SOUTH);


        pack();
        setVisible(true);
    }


    public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {


        final JFrame frame = new JFrame();
        // Release the window and quit the application when it has been closed
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        String name = JOptionPane.showInputDialog("What is your name?");
        String port = JOptionPane.showInputDialog("What is your port number?");
        String host = JOptionPane.showInputDialog("What is the name of the host?");

        frame.pack();
        frame.setVisible(true);


        client = new Client(port, name, host);

        //new ClientApp();

        ClientApp app = new ClientApp();
        app.setTitle(name);
        
        
    }

    public class ListListen implements ListSelectionListener{
        public void valueChanged(ListSelectionEvent arg0) {
            if (!arg0.getValueIsAdjusting()) {
                try {
                    String x = client.recieveMessage();
                    chat = client.getHistory(myList.getSelectedValue());
                    tp.setText(chat);


                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        }
              
        
    }


    public class ButtonListen implements ActionListener {
        public  void actionPerformed(ActionEvent e) {
            ((JButton)e.getSource()).setEnabled(true);
            String buttonText = ((JButton)e.getSource()).getText();

            if (buttonText.equals("Send Message")){
                try {
                    client.sendMessage(myList.getSelectedValue(), textField2.getText());
                    chat += '\n' + "You sent: " + textField2.getText();
                    String x = client.recieveMessage();
                    chat = client.getHistory(myList.getSelectedValue());
                    tp.setText(chat);

                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if (buttonText.equals("Refresh Client List")){
                try {
                    String clientList = client.getOtherClients();
                    if(!clientList.isEmpty())
                    {
                        listOfClients = clientList.split(", ");

                        DefaultListModel<String> model = new DefaultListModel<String>();
                        for (int i = 0; i <listOfClients.length; i++){
                            model.add(i, listOfClients[i]);
                        }
    
                        myList.setModel(model);
                    }
                



                } catch (IOException | InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

                if (buttonText.equals("Get Messages")){
                    try {
                        String x = client.recieveMessage();
                        chat = client.getHistory(myList.getSelectedValue());
                        tp.setText(chat);

    
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
            }

            if (buttonText.equals("Clear Chat")){  // no button yet exists
                
                chat = "";
                tp.setText(chat);
            }
        }


    }

}
