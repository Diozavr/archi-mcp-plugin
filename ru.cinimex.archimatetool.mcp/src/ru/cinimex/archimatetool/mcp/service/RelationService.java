package ru.cinimex.archimatetool.mcp.service;

import org.eclipse.swt.widgets.Display;

import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IArchimatePackage;

public class RelationService {

    public IArchimateRelationship createRelation(IArchimateModel model, String camelCaseType, String name, IArchimateElement source, IArchimateElement target, String folderId) {
        Object cls = IArchimatePackage.eINSTANCE.getEClassifier(camelCaseType);
        if (cls == null || !IArchimatePackage.eINSTANCE.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass) cls)) {
            throw new IllegalArgumentException("Unknown relation type: " + camelCaseType);
        }
        IArchimateRelationship rel = (IArchimateRelationship) IArchimateFactory.eINSTANCE.create((org.eclipse.emf.ecore.EClass) cls);
        rel.setName(name);
        rel.setSource(source);
        rel.setTarget(target);
        IFolder folder = resolveFolderFor(model, rel, folderId);
        final IFolder f = folder;
        Display.getDefault().syncExec(() -> f.getElements().add(rel));
        return rel;
    }

    public boolean deleteRelation(IArchimateRelationship rel) {
        if (rel != null && rel.getArchimateModel() != null) {
            ru.cinimex.archimatetool.mcp.server.ModelApi.removeRelationOccurrences(rel.getArchimateModel(), rel);
        }
        IFolder parent = (IFolder) rel.eContainer();
        if (parent == null) return false;
        final boolean[] res = new boolean[1];
        IFolder p = parent;
        Display.getDefault().syncExec(() -> res[0] = p.getElements().remove(rel));
        return res[0];
    }

    private IFolder resolveFolderFor(IArchimateModel model, com.archimatetool.model.IArchimateModelObject object, String folderId) {
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


