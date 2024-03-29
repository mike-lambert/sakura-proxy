package com.subgraph.orchid.circuits.hs;

import com.subgraph.orchid.Circuit;
import com.subgraph.orchid.Directory;
import com.subgraph.orchid.HiddenServiceCircuit;
import com.subgraph.orchid.InternalCircuit;
import com.subgraph.orchid.Router;
import com.subgraph.orchid.TorException;
import com.subgraph.orchid.circuits.CircuitManagerImpl;
import com.subgraph.orchid.crypto.TorTapKeyAgreement;
import com.subgraph.orchid.logging.Logger;

import java.util.concurrent.Callable;

public class RendezvousCircuitBuilder implements Callable<HiddenServiceCircuit> {
    private static final Logger logger = Logger.getInstance(RendezvousCircuitBuilder.class);

    private final Directory directory;

    private final CircuitManagerImpl circuitManager;
    private final HiddenService hiddenService;
    private final HSDescriptor serviceDescriptor;

    public RendezvousCircuitBuilder(Directory directory, CircuitManagerImpl circuitManager, HiddenService hiddenService, HSDescriptor descriptor) {
        this.directory = directory;
        this.circuitManager = circuitManager;
        this.hiddenService = hiddenService;
        this.serviceDescriptor = descriptor;
    }

    @Override
    public HiddenServiceCircuit call() throws Exception {

        logger.debug("Opening rendezvous circuit for " + logServiceName());

        final InternalCircuit rendezvous = circuitManager.getCleanInternalCircuit();
        logger.debug("Establishing rendezvous for " + logServiceName());
        RendezvousProcessor rp = new RendezvousProcessor(rendezvous);
        if (!rp.establishRendezvous()) {
            rendezvous.markForClose();
            return null;
        }
        logger.debug("Opening introduction circuit for " + logServiceName());
        final IntroductionProcessor introductionProcessor = openIntroduction();
        if (introductionProcessor == null) {
            logger.info("Failed to open connection to any introduction point");
            rendezvous.markForClose();
            return null;
        }
        logger.debug("Sending introduce cell for " + logServiceName());
        final TorTapKeyAgreement kex = new TorTapKeyAgreement();
        final boolean icResult = introductionProcessor.sendIntroduce(introductionProcessor.getServiceKey(), kex.getPublicKeyBytes(), rp.getCookie(), rp.getRendezvousRouter());
        introductionProcessor.markCircuitForClose();
        if (!icResult) {
            rendezvous.markForClose();
            return null;
        }
        logger.debug("Processing RV2 for " + logServiceName());
        HiddenServiceCircuit hsc = rp.processRendezvous2(kex);
        if (hsc == null) {
            rendezvous.markForClose();
        }

        logger.debug("Rendezvous circuit opened for " + logServiceName());

        return hsc;
    }

    private String logServiceName() {
        return hiddenService.getOnionAddressForLogging();
    }

    private IntroductionProcessor openIntroduction() {
        for (IntroductionPoint ip : serviceDescriptor.getShuffledIntroductionPoints()) {
            final Circuit circuit = attemptOpenIntroductionCircuit(ip);
            if (circuit != null) {
                return new IntroductionProcessor(hiddenService, circuit, ip);
            }
        }
        return null;
    }

    private Circuit attemptOpenIntroductionCircuit(IntroductionPoint ip) {
        final Router r = directory.getRouterByIdentity(ip.getIdentity());
        if (r == null) {
            return null;
        }

        try {
            final InternalCircuit circuit = circuitManager.getCleanInternalCircuit();
            return circuit.cannibalizeToIntroductionPoint(r);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (TorException e) {
            logger.debug("cannibalizeTo() failed : " + e.getMessage());
            return null;
        }
    }
}
