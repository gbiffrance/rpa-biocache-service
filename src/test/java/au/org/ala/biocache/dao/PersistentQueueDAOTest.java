package au.org.ala.biocache.dao;

import au.org.ala.biocache.dto.DownloadDetailsDTO;
import au.org.ala.biocache.dto.DownloadDetailsDTO.DownloadType;
import au.org.ala.biocache.dto.DownloadRequestParams;
import au.org.ala.biocache.dto.FacetThemes;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PersistentQueueDAOTest {

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);
    
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    
    private Path testCacheDir;
    private Path testDownloadDir;
    
    private PersistentQueueDAO queueDAO;
    
    @Before
    public void setUp() throws Exception{
        System.out.println("BEFORE...");

        //init FacetThemes
        new FacetThemes();

        testCacheDir = tempDir.newFolder("persistentqueuedaotest-cache").toPath();
        testDownloadDir = tempDir.newFolder("persistentqueuedaotest-destination").toPath();        
        queueDAO = new JsonPersistentQueueDAOImpl() {
            @Override
            public void init() {
                cacheDirectory = testCacheDir.toAbsolutePath().toString();
                biocacheDownloadDir = testDownloadDir.toAbsolutePath().toString();
                super.init();
            }
        };
        queueDAO.init();
    }

    @After
    public void tearDown() throws Exception {
        queueDAO.shutdown();
    }

    private DownloadRequestParams getParams(String query){
        DownloadRequestParams d = new DownloadRequestParams();
        d.setQ(query);
        d.setFile("Testing");
        d.setEmail("natasha.carter@csiro.au");
        return d;
    }
    
    @Test
    public void testQueue(){
        System.out.println("test add");
        DownloadDetailsDTO dd = new DownloadDetailsDTO(getParams("test1"), "127.0.0.1", "", DownloadType.FACET);
        
        queueDAO.addDownloadToQueue(dd);
        assertEquals(1,queueDAO.getTotalDownloads());
        DownloadDetailsDTO dd2 = new DownloadDetailsDTO(getParams("test2"), "127.0.0.1", "", DownloadType.FACET);
        
        queueDAO.addDownloadToQueue(dd2);
        assertEquals(2,queueDAO.getTotalDownloads());
        //now test that they are persisted
        queueDAO.refreshFromPersistent();
        assertEquals(2,queueDAO.getTotalDownloads());

        //now remove
        queueDAO.removeDownloadFromQueue(queueDAO.getNextDownload(Integer.MAX_VALUE, DownloadType.FACET));
        assertEquals(1,queueDAO.getTotalDownloads());
        //now test that the removal has been persisted
        queueDAO.refreshFromPersistent();
        assertEquals(1,queueDAO.getTotalDownloads());
    }
}
