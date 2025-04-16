/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package es.unizar.gui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class ScaleAndTranslate extends JDialog {

    private static final long serialVersionUID = 1L;
    public double scaleValue;
    public double xValue;
    public double yValue;

    public ScaleAndTranslate(JFrame parent) {
        super(parent, true);
        initComponents();
        pack();
        Rectangle parentBounds = parent.getBounds();
        Dimension size = getSize();
        // Center in the parent
        int x = Math.max(0, parentBounds.x + (parentBounds.width - size.width) / 2);
        int y = Math.max(0, parentBounds.y + (parentBounds.height - size.height) / 2);
        setLocation(new Point(x, y));
    }

    private void initComponents() {
        scaleTranslateButton = new javax.swing.JButton();
        scaleLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        scale = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        x = new javax.swing.JTextField();
        y = new javax.swing.JTextField();
        xLabel = new javax.swing.JLabel();
        yLabel = new javax.swing.JLabel();



        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Scale and Translate Coordinates");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        scaleTranslateButton.setText("Save");
        scaleTranslateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scaleTranslateButtonActionPerformed(evt);
            }
        });

        scaleLabel.setText("Scale");

        jLabel1.setForeground(new java.awt.Color(153, 153, 153));
        jLabel1.setText("Decimal value that specifies the new scale (1 is 100%)");

        jLabel2.setText("Coordinates of the translation");

        xLabel.setText("X");

        yLabel.setText("Y");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(scaleTranslateButton).addGroup(layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(xLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(x, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18).addComponent(yLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createSequentialGroup().addComponent(scaleLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scale, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18).addComponent(jLabel1))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(scaleLabel)
                .addComponent(jLabel1)
                .addComponent(scale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(x, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(y, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(xLabel)
                .addComponent(yLabel))
                .addGap(27, 27, 27)
                .addComponent(scaleTranslateButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }

    private void scaleTranslateButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (!scale.getText().isEmpty()) {
            scaleValue = Double.valueOf(scale.getText());
        } else {
            scaleValue = 1;
        }

        if (!x.getText().isEmpty()) {
            xValue = Double.valueOf(x.getText());
        } else {
            xValue = 0.0;
        }

        if (!y.getText().isEmpty()) {
            yValue = Double.valueOf(y.getText());
        } else {
            yValue = 0.0;
        }
        setVisible(false);
        dispose();
    }
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField scale;
    private javax.swing.JLabel scaleLabel;
    private javax.swing.JButton scaleTranslateButton;
    private javax.swing.JTextField x;
    private javax.swing.JLabel xLabel;
    private javax.swing.JTextField y;
    private javax.swing.JLabel yLabel;
}
