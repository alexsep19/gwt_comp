package gx.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.logging.client.ConsoleLogHandler;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.sencha.gxt.core.client.Style.SelectionMode;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.Store;
import com.sencha.gxt.data.shared.loader.ListLoadConfig;
import com.sencha.gxt.data.shared.loader.ListLoadResult;
import com.sencha.gxt.data.shared.loader.ListLoader;
import com.sencha.gxt.data.shared.loader.LoadResultListStoreBinding;
import com.sencha.gxt.data.shared.loader.RequestFactoryProxy;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.FramedPanel;
import com.sencha.gxt.widget.core.client.Dialog.PredefinedButton;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.button.ToolButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer.VerticalLayoutData;
import com.sencha.gxt.widget.core.client.event.BeforeStartEditEvent;
import com.sencha.gxt.widget.core.client.event.CancelEditEvent;
import com.sencha.gxt.widget.core.client.event.CompleteEditEvent;
import com.sencha.gxt.widget.core.client.event.DialogHideEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.StartEditEvent;
import com.sencha.gxt.widget.core.client.event.BeforeStartEditEvent.BeforeStartEditHandler;
import com.sencha.gxt.widget.core.client.event.CancelEditEvent.CancelEditHandler;
import com.sencha.gxt.widget.core.client.event.CompleteEditEvent.CompleteEditHandler;
import com.sencha.gxt.widget.core.client.event.DialogHideEvent.DialogHideHandler;
import com.sencha.gxt.widget.core.client.event.SelectEvent.SelectHandler;
import com.sencha.gxt.widget.core.client.event.StartEditEvent.StartEditHandler;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.Grid.GridCell;
import com.sencha.gxt.widget.core.client.grid.GridSelectionModel;
import com.sencha.gxt.widget.core.client.grid.editing.ClicksToEdit;
import com.sencha.gxt.widget.core.client.grid.editing.GridEditing;
import com.sencha.gxt.widget.core.client.grid.editing.GridRowEditing;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PanList<T> extends FramedPanel{
//public class PanList<T> extends ContentPanel{
	private static Logger rootLogger = Logger.getLogger("");
    protected GridEditing<T> editing;
    protected ToolButton tbIns;
    protected ToolButton tbDel;
    protected List<ColumnConfig<T, ?>> ccL = new ArrayList<ColumnConfig<T, ?>>();
    protected RequestFactoryProxy<ListLoadConfig, ListLoadResult<T>> rfpT;
    protected ListStore<T> stT;// = new ListStore<T>(propML.id());
    protected Grid<T> G;
    protected boolean isIns = false;
    VerticalLayoutContainer vcon = new VerticalLayoutContainer();

    protected Receiver<Void> mergReceiver = new Receiver<Void>() {
	      public void onSuccess(Void data) {
	         editing.cancelEditing();
		     G.getLoader().load();
		     setEditMode(false);
	       }
	      public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
	         StringBuffer b = new StringBuffer(); 
	       	 for (ConstraintViolation<?> err : violations){
		      	  b.append(err.getPropertyPath() + " : " + err.getMessage());
		       	}
		      AlertMessageBox d = new AlertMessageBox("Ошибка", b.toString());
				        d.show();
		      setEditMode(false);
		        }
	      public void onFailure(ServerFailure error) {
		      AlertMessageBox d = new AlertMessageBox("Ошибка", error.toString());
		      d.show();
		      super.onFailure(error);
		      setEditMode(false);
          }  };

    protected Receiver<Void> delReceiver = new Receiver<Void>() {
	  public void onSuccess(Void data) {
	      editing.cancelEditing();
	      G.getLoader().load();
		  }
		  public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
		     StringBuffer b = new StringBuffer(); 
		     for (ConstraintViolation<?> err : violations){
		        b.append(err.getPropertyPath() + " : " + err.getMessage());
		        }
		     AlertMessageBox d = new AlertMessageBox("Ошибка", b.toString());
		        d.show();
	 		     setEditMode(true);
		     }
	          public void onFailure(ServerFailure error) {
	             String s = error.getMessage();
	             if (s.indexOf("ORA-02292")>0) s = "Эта запись используется в других таблицах"; 
		     AlertMessageBox d = new AlertMessageBox("Охереная Ошибка", error.getMessage());
		        d.show();
		     super.onFailure(error);
	 		     setEditMode(true);
		     }		};
//---------------------------------------------------------
    public PanList(int w, int h, String Title) {
    	rootLogger.addHandler(new ConsoleLogHandler());
        addStyleName("margin-10");
        setHeadingText(Title);
        setPixelSize(w, h);

    }

    public void initValues(boolean firstLoad, boolean isEdit, boolean isBorder){
	ColumnModel<T> cmML = new ColumnModel<T>(ccL);
        final ListLoader<ListLoadConfig, ListLoadResult<T>> loaderT = new ListLoader<ListLoadConfig, ListLoadResult<T>>(rfpT);
        loaderT.addLoadHandler(new LoadResultListStoreBinding<ListLoadConfig, T, ListLoadResult<T>>(stT));
        loaderT.setRemoteSort(true);

        G = new Grid<T>(stT, cmML) {
            @Override
            protected void onAfterFirstAttach() {
              super.onAfterFirstAttach();
              Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
         	   loaderT.load();
                } }); }};
//        G.getView().setAutoExpandColumn(ccLine);
        G.getView().setForceFit(true);
//                G.getView().setStripeRows(true);
//                G.getView().setColumnLines(true);

        G.setLoadMask(true);
        G.setLoader(loaderT);
        G.mask("Загрузка");
//        final GridSelectionModel<T> sm = G.getSelectionModel();
//        sm.setSelectionMode(SelectionMode.SINGLE);
//        G.setSelectionModel(sm);
        //========// EDITING//
        if (isEdit){
           editing = createGridEditing(G);
     	   editing.addBeforeStartEditHandler(new BeforeStartEditHandler<T>(){
     	    @Override
     	      public void onBeforeStartEdit(BeforeStartEditEvent<T> event) {
     		  setEditMode(true);
     	      }});
     	   editing.addStartEditHandler(new StartEditHandler<T>(){
	       @Override
	       public void onStartEdit(StartEditEvent<T> event) {
		   beforEdit();
	       }
     	   });
           editing.addCompleteEditHandler(new CompleteEditHandler<T>(){
     	    @Override
     	    public void onCompleteEdit(CompleteEditEvent<T> event) {
     	       Store<T>.Record rec = stT.getRecord(stT.get(event.getEditCell().getRow()));
     	       if (!isIns && !rec.isDirty()) {editing.cancelEditing(); setEditMode(false); return;} 
     	       mergItem(rec.getModel());
     	       G.getLoader().load();
     	    }
             });
           editing.addCancelEditHandler(new CancelEditHandler<T>(){
     	    @Override
     	    public void onCancelEdit(CancelEditEvent<T> event) {
     		if (isIns){
     		  stT.remove(0);
     	          isIns = false;
     		}
     	       editing.cancelEditing();
     	       setEditMode(false);
     	    } });

        tbIns= new ToolButton(ToolButton.PLUS, new SelectHandler() {
              @Override  
              public void onSelect(SelectEvent event) {   
             	isIns = true;
             	rootLogger.log(Level.INFO, "Ins press");
                editing.cancelEditing();
             	insItem();
             	rootLogger.log(Level.INFO, "insItem() called");
                editing.startEditing(new GridCell(0, 0));
             	rootLogger.log(Level.INFO, "Ins end");
                 }    });
        tbIns.setTitle("Добавить");
             
        tbDel = new ToolButton(ToolButton.MINUS, new SelectHandler() {
              @Override  
              public void onSelect(SelectEvent event) {   
            	 editing.cancelEditing();
             	 if (G.getSelectionModel().getSelectedItems().size() > 0 ){
             	   final T item = G.getSelectionModel().getSelectedItem();
             	   final ConfirmMessageBox messageBox = new ConfirmMessageBox("Удаление", "Удалить " + getItemName(item) + "?");
             	   messageBox.addDialogHideHandler(new DialogHideHandler() {
//     			@Override
//     			       public void onHide(HideEvent event) {
//     			           Dialog btn = (Dialog) event.getSource();
//     	        	          if (btn.getHideButton().getText().equals("Yes")) {
//     	        	            delItem(item, delReceiver);
//     		                }
//     			       }

					public void onDialogHide(DialogHideEvent event) {
						if (event.getHideButton() == PredefinedButton.YES) delItem(item, delReceiver);
					      }
             	      });
             	      messageBox.show();
                     }
                 }    });
        tbDel.setTitle("Удалить");
        getHeader().addTool(tbIns);
        getHeader().addTool(tbDel);
        }
        if (firstLoad) G.getLoader().load();
        if (isBorder)  vcon.setBorders(true);
        vcon.add(G, new VerticalLayoutData(1, 1));
        setWidget(vcon);
    }
    

    private GridEditing<T> createGridEditing(Grid<T> Gr) {
	GridRowEditing<T> gre = new GridRowEditing<T>(Gr);
	gre.setClicksToEdit(ClicksToEdit.TWO);
	return gre;
    }
    
    protected void setEditMode(boolean isEdit){
	tbIns.setVisible(!isEdit);
	tbDel.setVisible(!isEdit);
    }
    protected void mergItem(T rec){
    }
    protected void insItem(){}
    protected String getItemName(T item){return "";}
    protected void delItem(T rec, Receiver<Void> R){}
    protected void beforEdit(){}
    public void fill(){}
  //============== SET&GET    
    public GridEditing<T> getEditing() {
        return editing;
    }

    public void setEditing(GridEditing<T> editing) {
        this.editing = editing;
    }
    public List<ColumnConfig<T, ?>> getCcL() {
        return ccL;
    }

    public void setCcL(List<ColumnConfig<T, ?>> ccL) {
        this.ccL = ccL;
    }

    public void setRfpT(
	    	RequestFactoryProxy<ListLoadConfig, ListLoadResult<T>> rfpT) {
	        this.rfpT = rfpT;
	    }
    public ListStore<T> getStT() {
        return stT;
    }

    public void setStT(ListStore<T> stT) {
        this.stT = stT;
    }

	public Grid<T> getG() {
		return G;
	}

	public void setG(Grid<T> g) {
		G = g;
	}

}
