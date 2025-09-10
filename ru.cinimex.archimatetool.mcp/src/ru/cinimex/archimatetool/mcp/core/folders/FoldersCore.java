package ru.cinimex.archimatetool.mcp.core.folders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.ConflictException;
import ru.cinimex.archimatetool.mcp.core.errors.BadRequestException;
import ru.cinimex.archimatetool.mcp.core.types.EnsureFolderCmd;
import ru.cinimex.archimatetool.mcp.core.validation.Validators;
import ru.cinimex.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IFolder;

/** Core operations for folders. */
public class FoldersCore {

    private IArchimateModel requireModel() {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) {
            throw new ConflictException("no active model");
        }
        return model;
    }

    /** List root folders of the active model. */
    public List<Map<String,Object>> listFolders() {
        var model = requireModel();
        List<Map<String,Object>> roots = new ArrayList<>();
        for (Object f : model.getFolders()) {
            if (f instanceof IFolder folder) {
                roots.add(folderToNode(folder, ""));
            }
        }
        return roots;
    }

    private Map<String,Object> folderToNode(IFolder folder, String parentPath) {
        Map<String,Object> node = new HashMap<>();
        String path = parentPath == null || parentPath.isEmpty() ? folder.getName() : parentPath + "/" + folder.getName();
        node.put("id", folder.getId());
        node.put("name", folder.getName());
        node.put("path", path);
        List<Map<String,Object>> children = new ArrayList<>();
        for (Object ch : folder.getFolders()) {
            if (ch instanceof IFolder cf) {
                children.add(folderToNode(cf, path));
            }
        }
        node.put("children", children);
        return node;
    }

    /** Ensure that a folder path exists within the diagrams root. */
    public Map<String,Object> ensureFolder(EnsureFolderCmd cmd) {
        Validators.requireNonEmpty(cmd.path, "path");
        var model = requireModel();
        IFolder root = model.getFolder(FolderType.DIAGRAMS);
        if (root == null) {
            throw new BadRequestException("no diagrams folder");
        }
        String[] parts = cmd.path.split("/");
        final IFolder[] current = new IFolder[]{ root };
        for (String seg : parts) {
            if (seg == null || seg.isEmpty()) continue;
            IFolder found = null;
            for (Object f : current[0].getFolders()) {
                if (f instanceof IFolder cf && seg.equals(cf.getName())) { found = cf; break; }
            }
            if (found == null) {
                final String name = seg;
                final IFolder[] created = new IFolder[1];
                org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> {
                    IFolder nf = com.archimatetool.model.IArchimateFactory.eINSTANCE.createFolder();
                    nf.setName(name);
                    nf.setType(current[0].getType());
                    current[0].getFolders().add(nf);
                    created[0] = nf;
                });
                current[0] = created[0];
            } else {
                current[0] = found;
            }
        }
        Map<String,Object> node = new HashMap<>();
        node.put("id", current[0].getId());
        node.put("name", current[0].getName());
        node.put("path", cmd.path);
        node.put("children", new ArrayList<>());
        return node;
    }
}
