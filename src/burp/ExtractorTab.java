package burp;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ExtractorTab implements ITab {

    private IBurpExtenderCallbacks callbacks;
    private JSplitPane splitPane;
    private JPanel topPane;
    private JButton modifyRequests;
    private boolean extractorOn;
    private ExtractorEditor requestEditor;
    private ExtractorEditor responseEditor;
    private String extractedData = "";
    private Font normalFont;
    private Font boldFont;

    public ExtractorTab(byte[] response, byte[] request, String responseHost, String requestHost, final IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;

        this.topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.Y_AXIS));

        // Build split pane
        this.splitPane = new JSplitPane();
        this.splitPane.setResizeWeight(0.5);
        JPanel leftPane = new JPanel();
        leftPane.setLayout(new GridBagLayout());
        leftPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,4));
        this.splitPane.setLeftComponent(leftPane);
        JPanel rightPane = new JPanel();
        rightPane.setLayout(new GridBagLayout());
        rightPane.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));
        this.splitPane.setRightComponent(rightPane);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(2,2,2,2);


        // Make our headers
        JLabel requestHeader = new JLabel("Request");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        this.normalFont = requestHeader.getFont();
        this.boldFont = new Font(this.normalFont.getFontName(), Font.BOLD, this.normalFont.getSize());
        requestHeader.setFont(this.boldFont);
        leftPane.add(requestHeader, constraints);

        JLabel responseHeader = new JLabel("Response");
        constraints.gridx = 0;
        constraints.gridy = 0;
        responseHeader.setFont(this.boldFont);
        rightPane.add(responseHeader, constraints);

        // Made button to dictate whether or not the extension is active
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.modifyRequests = new JButton("Turn Extractor on");
        this.modifyRequests.setFont(this.boldFont);
        this.modifyRequests.setBackground(Color.LIGHT_GRAY);
        this.extractorOn = false;
        buttonPanel.add(this.modifyRequests);

        this.modifyRequests.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Change button state
                extractorOn = !extractorOn;

                // Change button appearance
                if (extractorOn) {
                    modifyRequests.setBackground(Color.GRAY);
                    modifyRequests.setText("Turn Extractor off");
                } else {
                    modifyRequests.setBackground(Color.LIGHT_GRAY);
                    modifyRequests.setText("Turn Extractor on");
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        // Create help button
        JButton helpButton = new JButton("?");
        buttonPanel.add(helpButton);

        JPopupMenu menu = new JPopupMenu();
        menu.add(getHelpContents());
        helpButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                menu.show(helpButton, e.getX(), e.getY());
            }
        });

        topPane.add(buttonPanel);
        topPane.add(splitPane);

        // Create two editor panels. One for requests and one for responses
        this.requestEditor = new ExtractorEditor(callbacks);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 0.5;
        constraints.weighty = 1;
        leftPane.add(requestEditor.getUIComponent(), constraints);

        this.responseEditor = new ExtractorEditor(callbacks);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 0.5;
        constraints.weighty = 1;
        rightPane.add(responseEditor.getUIComponent(), constraints);

        // If this tab was created from the menu, we should be able to populate the request and response
        if (response != null && request != null) {
            this.setRequestMessage(request, requestHost);
            this.setResponseMessage(response, responseHost);
        }


        callbacks.customizeUiComponent(this.topPane);
    }

    public void setRequestMessage(byte[] message, String host) {
        this.requestEditor.setTargetHost(host);
        this.requestEditor.fillTextArea(message);
    }

    public void setResponseMessage(byte[] message, String host) {
        this.responseEditor.setTargetHost(host);
        this.responseEditor.fillTextArea(message);
    }

    // Get the regex string to select the intended text for replacement in the request
    public String getRequestSelectionRegex() {
        return this.requestEditor.getSelectionRegex();
    }

    // Get the regex string to select the text we want to replay somewhere in our requests
    public String getResponseSelectionRegex() {
        return this.responseEditor.getSelectionRegex();
    }

    // Returns true if the user has made the extension currently active (right now by checking a box)
    public boolean shouldModifyRequests() {
        return this.extractorOn;
    }

    // Determine if the given URL is in scope as defined by suite scope or a custom host
    public boolean requestIsInScope(URL url, String host) {
        if (this.requestEditor.useSuiteScope()) {
            return this.callbacks.isInScope(url);
        } else {
            if (this.requestEditor.useRegexForTarget()) {
                Pattern targetPattern = Pattern.compile(this.requestEditor.getTargetHost());
                Matcher targetMatcher = targetPattern.matcher(host);

                return targetMatcher.find();
            } else {
                return host.equals(this.requestEditor.getTargetHost());
            }
        }
    }

    // Determine if the given URL is in scope as defined by suite scope or a custom host
    public boolean responseIsInScope(URL url, String host) {
        if (this.responseEditor.useSuiteScope()) {
            return this.callbacks.isInScope(url);
        } else {
            if (this.responseEditor.useRegexForTarget()) {
                Pattern targetPattern = Pattern.compile(this.responseEditor.getTargetHost());
                Matcher targetMatcher = targetPattern.matcher(host);

                return targetMatcher.find();
            } else {
                return host.equals(this.responseEditor.getTargetHost());
            }
        }
    }

    public String getExtractedData() {
        return this.extractedData;
    }

    public void setExtractedData(String data) {
        this.extractedData = data;
    }

    private JPanel getHelpContents() {
        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));

        // Add large title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        titlePanel.add(Box.createHorizontalGlue());
        JLabel titleLabel = new JLabel("Burp Extractor");
        titleLabel.setBorder(new EmptyBorder(0,0,0,0));
        titleLabel.setFont(new Font(this.normalFont.getName(), Font.BOLD, this.normalFont.getSize() + 2));
        titlePanel.add(titleLabel, Component.LEFT_ALIGNMENT);
        helpPanel.add(titlePanel);

        // Add main scroll pane
        JPanel helpScrollPanel = new JPanel();
        helpScrollPanel.setLayout(new BoxLayout(helpScrollPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(helpScrollPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(helpScrollPanel);
        scrollPane.setPreferredSize(new Dimension(345,200));
        helpPanel.add(scrollPane);

        // Add text selection instructions
        JLabel highlightTitle = new JLabel("Text Selection");
        highlightTitle.setBorder(new EmptyBorder(5,5,5,5));
        highlightTitle.setFont(this.boldFont);
        helpScrollPanel.add(highlightTitle);
        JLabel highlightInstructions = new JLabel("<html><body style='width: 250px'><p>Highlight the text in the " +
                "request you would like to replace with data from the response. In the response, highlight the text " +
                "you would like to extract and have inserted into the request. This will set the necessary regex to " +
                "be used for selecting text. This regex can be edited manually if necessary.</p></body></html>");
        helpScrollPanel.add(highlightInstructions);

        // Add scope instructions
        JLabel scopeTitle = new JLabel("Defining scope");
        scopeTitle.setBorder(new EmptyBorder(5,5,5,5));
        scopeTitle.setFont(this.boldFont);
        helpScrollPanel.add(scopeTitle);
        helpScrollPanel.add(new JLabel("<html><body style='width: 250px'><p>There are two options for defining " +
                "scope in Extractor. Either a user can select \"Use suite scope\" which will cause Extractor to " +
                "examine a request if it is in Burp Suite's scope. Otherwise, the \"Target host\" field will be " +
                "examined, and a message from a matching (non-regex) host will cause Extractor to examine it." +
                "</p></body></html>"));

        // Running Extractor
        JLabel runningTitle = new JLabel("Running Extractor");
        runningTitle.setBorder(new EmptyBorder(5,5,5,5));
        runningTitle.setFont(this.boldFont);
        helpScrollPanel.add(runningTitle);
        helpScrollPanel.add(new JLabel("<html><body style='width:250px'><p>To start altering requests with " +
                "Extractor, just click \"Turn Extractor on\". Extractor will begin examining in-scope requests and " +
                "responses which match the defined scope for text which matches the regex fields. Once Extractor " +
                "has found a match in a response, it will save the text, and write it to the selected location in the " +
                "request. Multiple Extractor tabs can be used at once, and will execute in the order they were created." +
                "</p></body></html>"));

        return helpPanel;
    }

    @Override
    public String getTabCaption() {
        return "Extractor";
    }

    @Override
    public Component getUiComponent() {
        return this.topPane;
    }
}