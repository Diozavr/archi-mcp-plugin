package ru.cinimex.archimatetool.mcp.service;

import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;

public class ViewService {

    public IDiagramModel createArchimateView(IArchimateModel model, String name) {
        IArchimateDiagramModel view = IArchimateFactory.eINSTANCE.createArchimateDiagramModel();
        view.setName(name);
        IFolder diagramsFolder = model.getDefaultFolderForObject(view);
        Display.getDefault().syncExec(() -> diagramsFolder.getElements().add(view));
        return view;
    }

    public IDiagramModelArchimateObject addElementToView(IDiagramModel view, IArchimateElement element, int x, int y, int w, int h) {
        return addElementToContainer(view, element, x, y, w, h);
    }

    public IDiagramModelArchimateObject addElementToContainer(IDiagramModelContainer container, IArchimateElement element, int x, int y, int w, int h) {
        final IDiagramModelArchimateObject[] result = new IDiagramModelArchimateObject[1];
        final IDiagramModelContainer target = container;
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                IDiagramModelArchimateObject dmo = ArchimateDiagramModelFactory.createDiagramModelArchimateObject(element);
                IBounds b = IArchimateFactory.eINSTANCE.createBounds(x, y, w, h);
                dmo.setBounds(b);
                target.getChildren().add(dmo);
                result[0] = dmo;
            }
        });
        return result[0];
    }

    public boolean deleteViewObject(IDiagramModelObject obj) {
        IDiagramModelContainer parent = (IDiagramModelContainer) obj.eContainer();
        if (parent == null) return false;
        final boolean[] res = new boolean[1];
        IDiagramModelContainer p = parent;
        Display.getDefault().syncExec(() -> res[0] = p.getChildren().remove(obj));
        return res[0];
    }

    public IDiagramModelObject moveObjectToContainer(IDiagramModelObject obj, IDiagramModelContainer newParent,
                                                     Integer x, Integer y, Integer w, Integer h) {
        if (obj == null || newParent == null) return null;
        final IDiagramModelObject target = obj;
        final IDiagramModelContainer container = newParent;
        final Integer nx = x, ny = y, nw = w, nh = h;
        Display.getDefault().syncExec(() -> {
            // Detach from old parent
            Object p = target.eContainer();
            if (p instanceof IDiagramModelContainer) {
                ((IDiagramModelContainer) p).getChildren().remove(target);
            }
            // Optional bounds update
            if (nx != null || ny != null || nw != null || nh != null) {
                int bx = nx != null ? nx.intValue() : target.getBounds().getX();
                int by = ny != null ? ny.intValue() : target.getBounds().getY();
                int bw = nw != null ? nw.intValue() : target.getBounds().getWidth();
                int bh = nh != null ? nh.intValue() : target.getBounds().getHeight();
                IBounds b = IArchimateFactory.eINSTANCE.createBounds(bx, by, bw, bh);
                target.setBounds(b);
            }
            // Attach to new parent
            container.getChildren().add(target);
        });
        return target;
    }

    public boolean deleteView(IDiagramModel view) {
        IFolder parent = (IFolder) view.eContainer();
        if (parent == null) return false;
        final boolean[] res = new boolean[1];
        IFolder p = parent;
        Display.getDefault().syncExec(() -> res[0] = p.getElements().remove(view));
        return res[0];
    }
}


