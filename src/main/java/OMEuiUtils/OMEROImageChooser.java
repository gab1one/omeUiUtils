/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package OMEuiUtils;



import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;


import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
//import javax.swing.event.TreeExpansionEvent;
//import javax.swing.event.TreeWillExpandListener;

import javax.swing.tree.DefaultMutableTreeNode;
//import javax.swing.tree.ExpandVetoException;

import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import omero.ServerError;
import omero.api.IContainerPrx;
import omero.api.ServiceFactoryPrx;

import omero.client;
import omero.model.Dataset;
import omero.model.Experimenter;
import omero.model.IObject;
import omero.model.Image;
import omero.model.Plate;
import omero.model.Project;
//import omero.model.Screen;
import omero.sys.ParametersI;
import pojos.DatasetData;
import pojos.ImageData;
import pojos.PlateData;
import pojos.ProjectData;
//import pojos.ScreenData;



/**
 *
 * @author imunro
 */



public class OMEROImageChooser extends JDialog implements ActionListener {

    
    private JTree tree;
    private ArrayList<Object> returned; 
    private TreePath expDsetPath;
    
    // 0 == image, 1== dataset, 2 = Plate
    // NB Not selectable   3 == project, 4 = Screen, 5 = user
    private int selectedType;
    
    public OMEROImageChooser(omero.client omeroclient, long userId, int selectedType )  {
      this(omeroclient, userId, selectedType, false, new Long(-1));
    }
    
    public OMEROImageChooser(omero.client omeroclient, long userId, boolean allowMultiple )  {
      this(omeroclient, userId, 0, allowMultiple, new Long(-1) );
    }
    
    // Expand dataset (single Image) 
    public OMEROImageChooser(omero.client omeroclient, long userId, Long dsetExpandId)  {
      this(omeroclient, userId, 0, false, dsetExpandId);
    }
    
    // Expand dataset (Images) 
    public OMEROImageChooser(omero.client omeroclient, long userId, boolean allowMultiple, Long dsetExpandId)  {
      this(omeroclient, userId, 0,  allowMultiple,  dsetExpandId );
    }

    public OMEROImageChooser(omero.client omeroclient, long userId, int selectedType, boolean allowMultiple,  Long dsetExpandId)  {
      
      this.selectedType = selectedType;
      
      returned = null;
      
      expDsetPath = null;
     
      setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      
      
      // Create a Dialog to display while tree is poulated
      final JDialog waitDialog = new JDialog(this, "Fetching data. Please Wait...", false);
    
      waitDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      //waitDialog.setUndecorated(true);
      waitDialog.pack();
      waitDialog.setSize(400,60);
      waitDialog.setLocationRelativeTo(null);
  
      waitDialog.setVisible(true); // show the dialog on the screen
      
      

      
      String name;
      try {
        
        this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        //ServiceFactoryPrx session = omeroclient.joinSession(sessionid);
        ServiceFactoryPrx session = omeroclient.getSession();
        
        IContainerPrx proxy = session.getContainerService();
        ParametersI param = new ParametersI();
       
        param.exp(omero.rtypes.rlong(userId));
        Experimenter exp = session.getAdminService().getExperimenter(userId);
        name = exp.getFirstName().getValue() + " " + exp.getLastName().getValue();
        
       // name = session.getAdminService().getEventContext().userName;
        datasetInfo userInfo = new datasetInfo(name, 0L, 5); // type 5 is a user Id 0
        DefaultMutableTreeNode userNode = new DefaultMutableTreeNode(userInfo);

        //create the tree by passing in the root node
        tree = new JTree(userNode);
        tree.setShowsRootHandles(true);
        //tree.setRootVisible( false );
        
                
        JScrollPane spane = new JScrollPane(tree);
        spane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " ") );
        spane.setMinimumSize(new Dimension(350,300));
        spane.setPreferredSize(new Dimension(350,600));
        
        spane.add(tree);
        spane.setViewportView(tree);
        
        JPanel buttonPanel = new JPanel();
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
      
        buttonPanel.add(cancelButton, BorderLayout.LINE_START);
        // register the ButtonFrame object as the listener for the JButton.
        cancelButton.addActionListener( this ); 
        
        JButton openButton = new JButton("Open");
        openButton.setActionCommand("Open");
      
        buttonPanel.add(openButton, BorderLayout.LINE_END);
        // register the ButtonFrame object as the listener for the JButton.
        openButton.addActionListener( this ); 
       
        add(buttonPanel, BorderLayout.SOUTH );
        add(spane);
        
        switch (selectedType) {
          case 1:  tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                   setTitle("Please select a Dataset");
                   //param.noLeaves(); //no images loaded, this is the default value.
                   break;
          case 2:  tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                   setTitle("Please select a Plate");
                   //datasetsList = proj.linkedPlateList;
                   //param.noLeaves(); //no images loaded, this is the default value.
                   break;
          default: if (allowMultiple)  {
                     tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
                     setTitle("Please select one or more Images");
                   }
                   else  {
                     tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
                     setTitle("Please select an Image");
                   }
                   // Add pre-expansion event listener
                   //tree.addTreeWillExpandListener(new MyTreeWillExpandListener());
                   param.leaves();  //indicate to load the images
                   break;
        }
        
        
       /* if (selectedType == 2)  {   // Plate requested
          
          
          List<IObject> screenList = proxy.loadContainerHierarchy(Screen.class.getName(), new ArrayList<Long>(), param);

          Collections.sort(screenList, new Comparator<IObject>() {
            @Override
            public int compare(IObject projOne, IObject projTwo) {
              return (new ScreenData((Screen)projOne).getName().compareToIgnoreCase(new ScreenData((Screen)projTwo).getName()));
            }
          }); 
          
          
          
          Iterator<IObject> i = screenList.iterator();
          ScreenData screen;
          java.util.Set<PlateData> plates;
          Iterator<PlateData> j;
          PlateData plate;
          PlateData pl;
          while (i.hasNext()) {
            screen = new ScreenData((Screen) i.next());

            plates = screen.getPlates();

            String screenName = screen.getName() + " [" + Integer.toString(plates.size()) + "]";
            long sId = screen.getId();
            datasetInfo screenInfo = new datasetInfo(screenName, sId, 4);    //type 4 is a screen
            DefaultMutableTreeNode projNode = new DefaultMutableTreeNode(screenInfo);

            List<PlateData> plateList = new ArrayList<PlateData>(plates);
            Collections.sort(plateList, new Comparator<PlateData>() {
              @Override
              public int compare(PlateData pOne, PlateData pTwo) {
                return ( ((PlateData)pOne).getName().compareToIgnoreCase( ((PlateData)pTwo).getName()));
              }
            }); 

            j = plateList.iterator();
            while (j.hasNext()) {
              plate = j.next();
              DefaultMutableTreeNode node = addPlate(plate);
              projNode.add(node);
            }  
            userNode.add(projNode);
          }

        }
        else {  */
          
          List<IObject> projectList = proxy.loadContainerHierarchy(Project.class.getName(), new ArrayList<Long>(), param);
          List<IObject> alldatasetsList = proxy.loadContainerHierarchy(Dataset.class.getName(), new ArrayList<Long>(), param);

          Collections.sort(projectList, new Comparator<IObject>() {
            @Override
            public int compare(IObject projOne, IObject projTwo) {
              return (new ProjectData((Project)projOne).getName().compareToIgnoreCase(new ProjectData((Project)projTwo).getName()));
            }
          }); 
          
          Iterator<IObject> i = projectList.iterator();
          ProjectData project;
          java.util.Set<DatasetData> datasets;
          Iterator<DatasetData> j;
          Iterator<IObject> k = alldatasetsList.iterator();
          DatasetData dataset;
          DatasetData dset;
          String projName;
          String projNamePlus;
          
          while (i.hasNext()) {
            project = new ProjectData((Project) i.next());

            datasets = project.getDatasets();
            projName = project.getName();
            projNamePlus = projName + " [" + Integer.toString(datasets.size()) + "]";
            long pId = project.getId();
            datasetInfo projInfo = new datasetInfo(projNamePlus, pId, 3);    //type 3 is a project
            DefaultMutableTreeNode projNode = new DefaultMutableTreeNode(projInfo);
            userNode.add(projNode);
            List<DatasetData> datasetList = new ArrayList<DatasetData>(datasets);
            Collections.sort(datasetList, new Comparator<DatasetData>() {
              @Override
              public int compare(DatasetData dOne, DatasetData dTwo) {
                return ( dOne.getName().compareToIgnoreCase( dTwo.getName()));
              }
            }); 

            j = datasetList.iterator();
            while (j.hasNext()) {
              dataset = j.next();
              DefaultMutableTreeNode node = addDataset(dataset);
              projNode.add(node);
              if (dataset.getId()== dsetExpandId)  {
                expDsetPath = new TreePath(node.getPath());
              }
              long dId = dataset.getId();
              for( int ad=0; ad < alldatasetsList.size(); ad++)  {
                alldatasetsList.get(ad).getId().getValue();
                if (alldatasetsList.get(ad).getId().getValue() == dId)  {
                  alldatasetsList.remove(ad);
                  break;
                }
              }
            }  
            
            
          }

          for (int d = 0; d < alldatasetsList.size(); d++)  {
            dset = new DatasetData((Dataset)alldatasetsList.get(d));
            DefaultMutableTreeNode node =  addDataset(dset);
            userNode.add(node);  
          }
       // }
           
        customCellRenderer renderer = new customCellRenderer();
        renderer.setIcons();
        tree.setCellRenderer(renderer);
        
        
     
        if (expDsetPath != null)  {
          tree.expandPath(expDsetPath);
          tree.scrollPathToVisible(expDsetPath);
        } 
        else  {
          tree.expandRow(0);
        }
      

      } catch (ServerError ex) {
        Logger.getLogger(OMEROImageChooser.class.getName()).log(Level.SEVERE, null, ex);
      }
      
     
      waitDialog.setVisible(false);
      waitDialog.dispose();
      
      setSize(400,400);
      setLocationRelativeTo(null);
     
      pack();
      
      setVisible(true);
      
    }
    
    public Image[] getSelectedImages()  {
      
      if (selectedType == 0 & returned != null)  {
        
        return returned.toArray(new Image[returned.size()]);
      }
      else {
        return new Image[0];
      }
    }
    
    public Dataset getSelectedDataset()  {
      
      if (selectedType == 1 & returned != null)  {   
        return (Dataset)returned.get(0);
      }
      else {
        return null;
      }   
    }
    
    public Plate getSelectedPlate()  {
      
      if (selectedType == 2 & returned != null)  {   
        return (Plate)returned.get(0);
      }
      else {
        return null;
      }   
    }
    
 
    private DefaultMutableTreeNode addPlate(PlateData plate)    {

      String pName = plate.getName();
      long pId = plate.getId();
      datasetInfo dsetInfo = new datasetInfo(pName, pId, 2 );  // type 2 is a plate
      dsetInfo.setObject(plate);
      DefaultMutableTreeNode node  = new DefaultMutableTreeNode(dsetInfo);
      
      return node;
    }

    private DefaultMutableTreeNode addDataset(DatasetData dataset)    {

      
      java.util.Set<ImageData> images = null;
      
      String dsetName = dataset.getName();
      if (selectedType == 0)  {
         images = dataset.getImages();
        dsetName += " [" + Integer.toString(images.size()) + "]";
      }

      long dId = dataset.getId();
      datasetInfo dsetInfo = new datasetInfo(dsetName, dId, 1 );  // type 1 is a dataset
      dsetInfo.setObject(dataset);
      DefaultMutableTreeNode node  = new DefaultMutableTreeNode(dsetInfo);
      if (selectedType == 0)  {
        node = addImages( dataset, node, images);
      }
      return node;
    }
    
    private DefaultMutableTreeNode addImages(DatasetData dataset, DefaultMutableTreeNode node, java.util.Set<ImageData> images)    {
      

      List<ImageData> imageList = new ArrayList<ImageData>(images);
      Collections.sort(imageList, new Comparator<ImageData>() {
        @Override
        public int compare(ImageData iOne, ImageData iTwo) {
          return (iOne.getName().compareToIgnoreCase(iTwo.getName()));
        }
      });
      
      Iterator<ImageData> j = imageList.iterator();
      ImageData image;
      while (j.hasNext()) {
        image = j.next();
        String imName = image.getName();
        Long imId = image.getId();
        datasetInfo imageInfo = new datasetInfo(imName, imId, 0 );  // type 0 is an image
        imageInfo.setObject(image);
        DefaultMutableTreeNode imNode = new DefaultMutableTreeNode(imageInfo);
        node.add(imNode);
      }
      return node;
      
    }

  @Override
  public void actionPerformed(ActionEvent e) {
    datasetInfo info = null;
    String command = e.getActionCommand();
    if (command.equals("Open")) {

      TreePath[] paths = tree.getSelectionPaths();
      
      ArrayList<Object> selected = new ArrayList<Object>();
      
      if (paths != null)  {
        for (TreePath path : paths) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) (path.getLastPathComponent());
          if (node.isLeaf()) {
            datasetInfo di = (datasetInfo) node.getUserObject();
            if (di.getType() == selectedType) 
              switch (selectedType)  {
                case 1:   selected.add(((DatasetData)di.getObject()).asDataset());
                break;
                case 2:   selected.add(((PlateData)di.getObject()).asPlate());
                break;  
                default:  selected.add(((ImageData)di.getObject()).asImage());
                break;
              }
          }
        }
      }
      if (!selected.isEmpty())  {
        returned = selected;
        setVisible(false);
        dispose();
      }
      
    }
    
    if (command.equals("Cancel")) {
      setVisible(false);
      dispose(); 
    }
  }
  
      
// Pre-expansion/collapse event listener
/*public class MyTreeWillExpandListener implements TreeWillExpandListener {
    public void treeWillExpand(TreeExpansionEvent evt) throws ExpandVetoException {
      JTree tree = (JTree) evt.getSource();

      // Get the path that will be expanded
      TreePath path = evt.getPath();
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      datasetInfo dInfo = (datasetInfo) node.getUserObject();
      int type = dInfo.getType();
      DefaultMutableTreeNode leaf = node.getFirstLeaf();
      if (type == 3) {     //Project
        for (int c = 0; c < node.getLeafCount(); c++) {
          datasetInfo lInfo = (datasetInfo) leaf.getUserObject();
          if (lInfo.getType() == 1) {  //Dataset
            DatasetData d = (DatasetData) lInfo.getObject();
            java.util.Set<ImageData> images = d.getImages();
            addImages(d, leaf, images);
          }
          leaf = node.getNextLeaf();
        } 
      } 


    } 

   public void treeWillCollapse(TreeExpansionEvent evt) throws ExpandVetoException {
        JTree tree = (JTree)evt.getSource();

        // Get the path that will be collapsed
        //TreePath path = evt.getPath();
        //Object e = evt.getSource();
       

        
    } 
}  */

    
    
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              
              String pass;
 
              Scanner in = new Scanner(System.in);
              
              System.out.println("Enter passwd");
              pass = in.nextLine();
              System.out.println("You entered "+pass);
              
              try {
                client omeroclient = new client("cell.bioinformatics.ic.ac.uk", 4064);
                
             

                ServiceFactoryPrx session = omeroclient.createSession("imunro", pass);
                
                long uId = session.getAdminService().getEventContext().userId;  
                
 
                if (omeroclient != null)  {
               
                 
                  OMEROImageChooser chooser = new OMEROImageChooser(omeroclient, uId, new Long(7641));
                  //OMEROImageChooser chooser = new OMEROImageChooser(omeroclient, 1 );
                 
                  //Dataset returned = chooser.getSelectedDataset();
                  //Plate returned = chooser.getSelectedPlate();
                  
                 
                 
                 // System.out.println(returned.getName().getValue());
                  
                  Image[] returned = chooser.getSelectedImages();
                  for (int i = 0; i < returned.length; i++) {
                    System.out.println(returned[i].getName().getValue());
                  }  
                
                  System.out.println("closing down");
                     

                  omeroclient.closeSession();
                }

              } catch (CannotCreateSessionException ex) {
                Logger.getLogger(OMEROImageChooser.class.getName()).log(Level.SEVERE, null, ex);
              } catch (PermissionDeniedException ex) {
                Logger.getLogger(OMEROImageChooser.class.getName()).log(Level.SEVERE, null, ex);
              } catch (ServerError ex) {
                Logger.getLogger(OMEROImageChooser.class.getName()).log(Level.SEVERE, null, ex);
              }
            }

        });
    }
}

