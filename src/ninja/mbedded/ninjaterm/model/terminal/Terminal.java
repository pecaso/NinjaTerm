package ninja.mbedded.ninjaterm.model.terminal;

import ninja.mbedded.ninjaterm.model.terminal.stats.Stats;
import ninja.mbedded.ninjaterm.model.terminal.txRx.TxRx;

/**
 * Created by gbmhu on 2016-09-16.
 */
public class Terminal {

    public TxRx txRx = new TxRx();
    public Stats stats = new Stats();

}