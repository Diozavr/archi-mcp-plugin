package com.archimatetool.mcp.service;

import org.eclipse.swt.widgets.Display;

import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IArchimatePackage;

public class ElementService {

    public IArchimateElement createElement(IArchimateModel model, String camelCaseType, String name, String folderId) {
        Object cls = IArchimatePackage.eINSTANCE.getEClassifier(camelCaseType);
        if (cls == null || !IArchimatePackage.eINSTANCE.getArchimateElement().isSuperTypeOf((org.eclipse.emf.ecore.EClass) cls)) {
            throw new IllegalArgumentException("Unknown element type: " + camelCaseType);
        }
        IArchimateElement el = (IArchimateElement) IArchimateFactory.eINSTANCE.create((org.eclipse.emf.ecore.EClass) cls);
        el.setName(name);
        IFolder folder = resolveFolderFor(model, el, folderId);
        final IFolder f = folder;
        Display.getDefault().syncExec(() -> f.getElements().add(el));
        return el;
    }

    public boolean deleteElement(IArchimateElement el) {
        // Ensure occurrences are removed from all views before deleting the element from its folder
        if (el != null && el.getArchimateModel() != null) {
            com.archimatetool.mcp.server.ModelApi.removeElementOccurrences(el.getArchimateModel(), el);
        }
        IFolder parent = (IFolder) el.eContainer();
        if (parent == null) return false;
        final boolean[] res = new boolean[1];
        IFolder p = parent;
        Display.getDefault().syncExec(() -> res[0] = p.getElements().remove(el));
        return res[0];
    }

    private IFolder resolveFolderFor(IArchimateModel model, IArchimateModelObject object, String folderId) {
        IFolder folder = null;
        if (folderId != null) {
            Object o = com.archimatetool.model.util.ArchimateModelUtils.getObjectByID(model, folderId);
            if (o instanceof IFolder) {
                folder = (IFolder) o;
            }
        }
        if (folder == null) {
            folder = model.getDefaultFolderForObject(object);
        }
        return folder;
    }
}


