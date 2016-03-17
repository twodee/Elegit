package main.java.elegit;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AuthenticatedCloneTest {

    private Path directoryPath;
    private String testFileLocation;


    @Before
    public void setUp() throws Exception {
        this.directoryPath = Files.createTempDirectory("unitTestRepos");
        directoryPath.toFile().deleteOnExit();
        testFileLocation = System.getProperty("user.home") + File.separator +
                           "elegitTests" + File.separator;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCloneHttpNoPassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        // Clone from dummy repo:
        String remoteURL = "https://github.com/TheElegitTeam/TestRepository.git";

        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL);
        assertNotNull(helper);

    }

    @Test
    public void testLsHttpNoPassword() throws Exception {

        TransportCommand command =
                Git.lsRemoteRepository().setRemote("https://github.com/TheElegitTeam/TestRepository.git");
        command.call();
    }

    /* The httpUsernamePassword should contain three lines, containing:
        repo http(s) address
        username
        password
     */
    @Test
    public void testHttpUsernamePassword() throws Exception {
        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);
        ClonedRepoHelper helper = new ClonedRepoHelper(repoPath, remoteURL, credentials);
    }

    @Test
    public void testLsHttpUsernamePassword() throws Exception {

        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "httpUsernamePassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String username = scanner.next();
        String password = scanner.next();
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, remoteURL, credentials);
        command.call();
    }

    @Test
    public void testLsSshPassword() throws Exception {

        Path repoPath = directoryPath.resolve("testrepo");
        File authData = new File(testFileLocation + "sshPassword.txt");

        // If a developer does not have this file present, test should just pass.
        if (!authData.exists())
            return;

        Scanner scanner = new Scanner(authData);
        String remoteURL = scanner.next();
        String password = scanner.next();

        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(username, password);

        TransportCommand command = Git.lsRemoteRepository().setRemote(remoteURL);
        RepoHelper.wrapAuthentication(command, remoteURL, credentials);
        command.call();
    }

}