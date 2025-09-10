package ru.cinimex.archimatetool.mcp.service;

import java.io.IOException;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.util.ArchimateModelUtils;

public class ActiveModelService implements IActiveModelService {

    @Override
    public IArchimateModel getActiveModel() {
        final IArchimateModel[] active = new IArchimateModel[1];
        if (PlatformUI.isWorkbenchRunning()) {
            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window != null) {
                        IWorkbenchPage page = window.getActivePage();
                        if (page != null) {
                            IWorkbenchPart part = page.getActivePart();
                            if (part != null) {
                                Object adapted = part.getAdapter(IArchimateModel.class);
                                if (adapted instanceof IArchimateModel) {
                                    active[0] = (IArchimateModel) adapted;
                                }
                            }
                        }
                    }
                }
            });
            if (active[0] != null) return active[0];
        }
        List<IArchimateModel> models = IEditorModelManager.INSTANCE.getModels();
        return models.isEmpty() ? null : models.get(0);
    }

    @Override
    public Object findById(IArchimateModel model, String id) {
        return ArchimateModelUtils.getObjectByID(model, id);
    }

    @Override
    public boolean saveActiveModel() {
        IArchimateModel model = getActiveModel();
        if (model == null || model.getFile() == null) return false;
        final boolean[] ok = new boolean[1];
        Display.getDefault().syncExec(new Runnable() { public void run() {
            try {
                IEditorModelManager.INSTANCE.saveModel(model);
                ok[0] = true;
            } catch (IOException e) {
                ok[0] = false;
            }
        }});
        return ok[0];
    }
}


