package com.t13g2.forum.logic.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;

import com.t13g2.forum.commons.core.Messages;
import com.t13g2.forum.logic.CommandHistory;
import com.t13g2.forum.logic.commands.exceptions.CommandException;
import com.t13g2.forum.model.Context;
import com.t13g2.forum.model.Model;
import com.t13g2.forum.model.ModelManager;
import com.t13g2.forum.model.UnitOfWork;
import com.t13g2.forum.model.UserPrefs;
import com.t13g2.forum.model.forum.Comment;
import com.t13g2.forum.model.forum.ForumThread;
import com.t13g2.forum.model.forum.Module;
import com.t13g2.forum.model.forum.User;
import com.t13g2.forum.testutil.CommentBuilder;
import com.t13g2.forum.testutil.ForumThreadBuilder;
import com.t13g2.forum.testutil.TypicalModules;
import com.t13g2.forum.testutil.TypicalPersons;
import com.t13g2.forum.testutil.TypicalUsers;

//@@author HansKoh
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteThreadCommandTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private Model model;
    private Model expectedModel;
    private CommandHistory commandHistory = new CommandHistory();

    @Before
    public void setUp() {
        model = new ModelManager(TypicalPersons.getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getForumBook(), new UserPrefs());
    }

    @Test
    public void execute_userLoggedInDeleteThread_deleteThreadSuccess() throws Exception {
        //set the current logged in user as a user.
        User validUser = TypicalUsers.JANEDOE;
        Context.getInstance().setCurrentUser(validUser);

        Module validModule = TypicalModules.CS1231;
        int moduleId = 0;
        int threadId = 0;
        try (UnitOfWork unitOfWork = new UnitOfWork()) {
            unitOfWork.getModuleRepository().addModule(validModule);
            unitOfWork.commit();
            moduleId = unitOfWork.getModuleRepository().getModuleByCode(validModule.getModuleCode()).getId();
            Context.getInstance().setCurrentModuleId(moduleId);
            ForumThread forumThread = new ForumThreadBuilder().withModuleId(moduleId).build();
            Comment comment = new CommentBuilder().withThreadId(forumThread.getId()).build();

            CreateThreadCommand createThreadCommand =
                    new CreateThreadCommand(validModule.getModuleCode(), forumThread, comment);
            createThreadCommand.execute(model, commandHistory);
            threadId = forumThread.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeleteThreadCommand deleteThreadCommand = new DeleteThreadCommand(threadId);
        String deletedMessage = "\n\n"
                + "Under Module Code: " + validModule.getModuleCode() + "\n"
                + "Deleted Thread ID: " + threadId + "\n";

        CommandTestUtil.assertCommandSuccess(deleteThreadCommand, model, commandHistory,
                String.format(deleteThreadCommand.MESSAGE_SUCCESS, deletedMessage), expectedModel);

    }

    @Test
    public void execute_userLoggedInNotThreadOwner_deleteThreadFailed() throws Exception {
        //set the current logged in user as a user.
        User validUser = TypicalUsers.JANEDOE;
        Context.getInstance().setCurrentUser(validUser);

        Module validModule = TypicalModules.CS1231;
        int moduleId = 0;
        int threadId = 0;
        try (UnitOfWork unitOfWork = new UnitOfWork()) {
            unitOfWork.getModuleRepository().addModule(validModule);
            unitOfWork.commit();
            moduleId = unitOfWork.getModuleRepository().getModuleByCode(validModule.getModuleCode()).getId();
            Context.getInstance().setCurrentModuleId(moduleId);
            ForumThread forumThread = new ForumThreadBuilder().withModuleId(moduleId).build();
            Comment comment = new CommentBuilder().withThreadId(forumThread.getId()).build();
            CreateThreadCommand createThreadCommand =
                    new CreateThreadCommand(validModule.getModuleCode(), forumThread, comment);
            createThreadCommand.execute(model, commandHistory);
            threadId = forumThread.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Context.getInstance().setCurrentUser(TypicalUsers.JIM);
        DeleteThreadCommand deleteThreadCommand = new DeleteThreadCommand(threadId);

        thrown.expect(CommandException.class);
        thrown.expectMessage(Messages.MESSAGE_NOT_THREAD_OWNER);

        CommandResult commandResult = deleteThreadCommand.execute(model, commandHistory);
        assertEquals(Messages.MESSAGE_NOT_THREAD_OWNER, commandResult.feedbackToUser);

    }

    @Test
    public void execute_userLoggedInNotUnderCurrentScope_deleteThreadFailed() throws Exception {
        //set the current logged in user as a user.
        User validUser = TypicalUsers.JANEDOE;
        Context.getInstance().setCurrentUser(validUser);

        Module validModule = TypicalModules.CS1231;
        int moduleId = 0;
        int threadId = 0;
        try (UnitOfWork unitOfWork = new UnitOfWork()) {
            unitOfWork.getModuleRepository().addModule(validModule);
            unitOfWork.commit();
            moduleId = unitOfWork.getModuleRepository().getModuleByCode(validModule.getModuleCode()).getId();
            Context.getInstance().setCurrentModuleId(8888);
            ForumThread forumThread = new ForumThreadBuilder().withModuleId(moduleId).build();
            Comment comment = new CommentBuilder().withThreadId(forumThread.getId()).build();

            CreateThreadCommand createThreadCommand =
                    new CreateThreadCommand(validModule.getModuleCode(), forumThread, comment);
            createThreadCommand.execute(model, commandHistory);
            threadId = forumThread.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeleteThreadCommand deleteThreadCommand = new DeleteThreadCommand(threadId);

        thrown.expect(CommandException.class);
        thrown.expectMessage(Messages.MESSAGE_INVALID_THREAD);

        CommandResult commandResult = deleteThreadCommand.execute(model, commandHistory);
        assertEquals(Messages.MESSAGE_INVALID_THREAD, commandResult.feedbackToUser);
    }

    @Test
    public void execute_notLoggedInDeleteThread_deleteThreadFailed() throws Exception {
        //set the current logged in user as null.
        Context.getInstance().setCurrentUser(null);

        Module validModule = TypicalModules.CS1231;
        ForumThread forumThread = new ForumThreadBuilder().withModuleId(validModule.getId()).build();
        DeleteThreadCommand deleteThreadCommand =
                new DeleteThreadCommand(forumThread.getId());

        thrown.expect(CommandException.class);
        thrown.expectMessage(Messages.MESSAGE_NOT_LOGIN);

        CommandResult commandResult = deleteThreadCommand.execute(model, commandHistory);
        assertEquals(Messages.MESSAGE_NOT_LOGIN, commandResult.feedbackToUser);

    }
}
