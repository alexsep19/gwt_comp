package gx.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.Store;
import com.sencha.gxt.data.shared.loader.FilterPagingLoadConfig;
import com.sencha.gxt.data.shared.loader.FilterPagingLoadConfigBean;
import com.sencha.gxt.data.shared.loader.LoadResultListStoreBinding;
import com.sencha.gxt.data.shared.loader.PagingLoadResult;
import com.sencha.gxt.data.shared.loader.PagingLoader;
import com.sencha.gxt.data.shared.loader.RequestFactoryProxy;
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
import com.sencha.gxt.widget.core.client.grid.editing.ClicksToEdit;
import com.sencha.gxt.widget.core.client.grid.editing.GridEditing;
import com.sencha.gxt.widget.core.client.grid.editing.GridRowEditing;
import com.sencha.gxt.widget.core.client.grid.filters.GridFilters;
import com.sencha.gxt.widget.core.client.toolbar.PagingToolBar;

public class PanPage<T> extends FramedPanel{
    protected GridEditing<T> editing;
    protected ToolButton tbIns;
    protected ToolButton tbDel;
    protected Grid<T> grT;
	protected List<ColumnConfig<T, ?>> ccL = new ArrayList<ColumnConfig<T, ?>>();
    protected GridFilters<T> filters;
    protected RequestFactoryProxy<FilterPagingLoadConfig, PagingLoadResult<T>> rfpT;
    protected ListStore<T> stT;
    protected boolean isIns = false;
    protected int page;
    VerticalLayoutContainer vcon = new VerticalLayoutContainer();
    protected Receiver<Void> mergReceiver = new Receiver<Void>() {
	      public void onSuccess(Void data) {
	         editing.cancelEditing();
	         grT.getLoader().load();
		 setEditMode(false);
	       }
	      public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
	         StringBuffer b = new StringBuffer(); 
	       	 for (ConstraintViolation<?> err : violations){
		      	  b.append(err.getPropertyPath() + " : " + err.getMessage());
		       	}
		      AlertMessageBox d = new AlertMessageBox("Ошибка", b.toString());
				        d.show();
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
	      grT.getLoader().load();
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

//=================================================    
    public PanPage(int w, int h, int Page, String Title) {
        addStyleName("margin-10");
        setHeadingText(Title);
        getHeader().addStyleName("txt_center");
        setPixelSize(w, h);
        page = Page;
    }
    public void initValues(boolean firstLoad, boolean isEdit){
	ColumnModel<T> cmT = new ColumnModel<T>(ccL);
        final PagingLoader<FilterPagingLoadConfig, PagingLoadResult<T>> loader = 
        	new PagingLoader<FilterPagingLoadConfig, PagingLoadResult<T>>(rfpT) {
        	      @Override protected FilterPagingLoadConfig newLoadConfig() {return new FilterPagingLoadConfigBean();} };
 	loader.setRemoteSort(true);
        loader.addLoadHandler(new LoadResultListStoreBinding<FilterPagingLoadConfig, T, PagingLoadResult<T>>(stT));
        final PagingToolBar toolBar = new PagingToolBar(page);
        toolBar.getElement().getStyle().setProperty("borderBottom", "none");
        toolBar.bind(loader);

        grT = new Grid<T>(stT, cmT) {
            @Override
            protected void onAfterFirstAttach() {
              super.onAfterFirstAttach();
              Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                  loader.load();
                }
              }); }};
        grT.mask("Загрузка");
        grT.getView().setForceFit(true);
        grT.setLoadMask(true);
        grT.setLoader(loader);
        filters = new GridFilters<T>(loader);
        filters.initPlugin(grT);
        filters.setLocal(false);//be sure to be remote, or it will affect the local cached data only
      //========// EDITING//
        if (isEdit){
            editing = createGridEditing(grT);
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
     	       grT.getLoader().load();
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
            @Override  public void onSelect(SelectEvent event) {   
           	isIns = true;
              editing.cancelEditing();
           	insItem();
              editing.startEditing(new GridCell(0, 0));
               }    });
            tbIns.setTitle("Добавить");
            tbDel = new ToolButton(ToolButton.MINUS, new SelectHandler() {
            @Override  public void onSelect(SelectEvent event) {   
          	 editing.cancelEditing();
           	 if (grT.getSelectionModel().getSelectedItems().size() > 0 ){
           	   final T item = grT.getSelectionModel().getSelectedItem();
           	   final ConfirmMessageBox messageBox = new ConfirmMessageBox("Удаление", "Удалить " + getItemName(item) + "?");
         	   messageBox.addDialogHideHandler(new DialogHideHandler() {
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
      if (firstLoad) grT.getLoader().load();
      vcon.setBorders(true);
      vcon.add(grT, new VerticalLayoutData(1, 1));
      vcon.add(toolBar, new VerticalLayoutData(1, -1));
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
    protected void mergItem(T rec){}
    protected void insItem(){}
    protected String getItemName(T item){return "";}
    protected void delItem(T rec, Receiver<Void> R){}
    protected void beforEdit(){}
    public void fill(){}
    //============== SET&GET    
    public List<ColumnConfig<T, ?>> getCcL() {
        return ccL;
    }
    public void setCcL(List<ColumnConfig<T, ?>> ccL) {
        this.ccL = ccL;
    }
    public GridEditing<T> getEditing() {
        return editing;
    }
    public void setEditing(GridEditing<T> editing) {
        this.editing = editing;
    }
    public RequestFactoryProxy<FilterPagingLoadConfig, PagingLoadResult<T>> getRfpT() {
        return rfpT;
    }
    public void setRfpT(RequestFactoryProxy<FilterPagingLoadConfig, PagingLoadResult<T>> rfpT) {
        this.rfpT = rfpT;
    }
    public ListStore<T> getStT() {
        return stT;
    }
    public void setStT(ListStore<T> stT) {
        this.stT = stT;
    }
    public GridFilters<T> getFilters() {
        return filters;
    }
    public void setFilters(GridFilters<T> filters) {
        this.filters = filters;
    }
    public Grid<T> getGrT() {
		return grT;
	}
	public void setGrT(Grid<T> grT) {
		this.grT = grT;
	}
}
