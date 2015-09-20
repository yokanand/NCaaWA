/*
 * This file is part of the Disco Deterministic Network Calculator v2.0.3 "Hydra".
 *
 * Copyright (C) 2013, 2014 Steffen Bondorf
 *
 * disco | Distributed Computer Systems Lab
 * University of Kaiserslautern, Germany
 *
 * http://disco.cs.uni-kl.de
 *
 *
 * The Disco Deterministic Network Calculator (DiscoDNC) is free software;
 * you can redistribute it and/or modify it under the terms of the 
 * GNU Lesser General Public License as published by the Free Software Foundation; 
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */

package unikl.disco.dnc.server.tests;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import unikl.disco.dnc.server.nc.Analysis;
import unikl.disco.dnc.shared.Configuration;
import unikl.disco.dnc.shared.Configuration.ArrivalBoundMethods;
import unikl.disco.dnc.shared.Configuration.MuxDiscipline;
import unikl.disco.dnc.shared.Num;
import unikl.disco.dnc.shared.curves.ArrivalCurve;
import unikl.disco.dnc.shared.curves.ServiceCurve;
import unikl.disco.dnc.shared.network.Flow;
import unikl.disco.dnc.shared.network.Network;
import unikl.disco.dnc.shared.network.Server;
import unikl.disco.dnc.shared.results.PmooAnalysisResults;
import unikl.disco.dnc.shared.results.SeparateFlowAnalysisResults;
import unikl.disco.dnc.shared.results.TotalFlowAnalysisResults;

@RunWith(value = Parameterized.class)
/**
 * 
 * @author Steffen Bondorf
 *
 */
public class Single_2Flows_2ACs
{
	Configuration config;
	
	static final ServiceCurve service_curve = ServiceCurve.createRateLatency( 10, 10 );
	static final ArrivalCurve arrival_curve_0 = ArrivalCurve.createTokenBucket( 4, 10 );
	static final ArrivalCurve arrival_curve_1 = ArrivalCurve.createTokenBucket( 5, 25 );
	static final ServiceCurve max_service_curve = ServiceCurve.createRateLatency( 15, 5 );
	
	static Network network;
	static Server s0;
	static Flow f0, f1;
	
	@Parameters
	public static Collection<Object[]> data() {
		return FunctionalTests.createParameters();
	}
	 
	public Single_2Flows_2ACs( HashSet<ArrivalBoundMethods> arrival_boundings, boolean iterative_ab, boolean remove_duplicates ) {
		config = FunctionalTests.printTestSettings( arrival_boundings, iterative_ab, remove_duplicates );
	}
	
	@BeforeClass
	static public void createNetwork()
	{
		network = new Network();
		
		s0 = network.addServer( service_curve, max_service_curve );
		s0.setUseGamma( false );
		s0.setUseExtraGamma( false );
	
		try {	
			f0 = network.addFlow( arrival_curve_0, s0 );	
			f1 = network.addFlow( arrival_curve_1, s0 );
		} catch (Exception e) {
			System.out.println( e.toString() );
			assertEquals( "Unexpected exception occured", 0, 1 );
			return;
		}
	}
	
//--------------------Flow 0--------------------
	@Test
	public void f0_tfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseFifoMultiplexing( true ); // default = false
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f0 );
		
		if ( tfa_results.failure == true ) {
			System.out.println( "TFA analysis failed" );
			System.out.println();
			
			if( !config.arrivalBoundMethods().contains( ArrivalBoundMethods.PMOO ) ) {
				assertEquals( "Unexpected exception occured", 0, 1 );
			}
			
			return;
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tTotal Flow Analysis (TFA)" );
			System.out.println( "Multiplexing:\t\tFIFO" );
	
			System.out.println( "Flow of interest:\t" + f0.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}

		assertEquals( "TFA FIFO delay", new Num( 13.5 ), tfa_results.delay_bound );
		assertEquals( "TFA FIFO backlog", new Num( 125 ), tfa_results.backlog_bound );
	}

	@Test
	public void f0_tfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true ); // s0.useArbitraryMultiplexing() != s0.useFifoMultiplexing()
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f0 );
		
		if ( tfa_results.failure == true ) {
			System.out.println( "TFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tTotal Flow Analysis (TFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f0.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}

		assertEquals( "TFA ARB delay", new Num( 135 ), tfa_results.delay_bound );
		assertEquals( "TFA ARB backlog", new Num( 125 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f0_sfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseFifoMultiplexing( true );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f0 );
		
		if ( sfa_results.failure == true ) {
			System.out.println( "SFA analysis failed" );
			System.out.println();
			
			if( !config.arrivalBoundMethods().contains( ArrivalBoundMethods.PMOO ) ) {
				assertEquals( "Unexpected exception occured", 0, 1 );
			}
			
			return;
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tSeparate Flow Analysis (SFA)" );
			System.out.println( "Multiplexing:\t\tFIFO" );
	
			System.out.println( "Flow of interest:\t" + f0.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}

		assertEquals( "SFA FIFO delay", new Num( 29, 2 ), sfa_results.delay_bound );
		assertEquals( "SFA FIFO backlog", new Num( 60 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f0_sfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f0 );
		
		if ( sfa_results.failure == true ) {
			System.out.println( "SFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tSeparate Flow Analysis (SFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f0.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}

		assertEquals( "SFA ARB delay", new Num( 27 ), sfa_results.delay_bound );
		assertEquals( "SFA ARB backlog", new Num( 110 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f0_pmoo_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true );
		
		PmooAnalysisResults pmoo_results = Analysis.performPmooEnd2End( network, config, f0 );
		
		if ( pmoo_results.failure == true ) {
			System.out.println( "PMOO analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tPay Multiplexing Only Once (PMOO)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f0.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );

			System.out.println( "e2e PMOO SCs    : " + pmoo_results.betas_e2e );
			System.out.println( "xtx per server  : " + pmoo_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + pmoo_results.delay_bound );
			System.out.println( "backlog bound   : " + pmoo_results.backlog_bound );
		}
		
		assertEquals( "PMOO ARB delay", new Num( 27 ), pmoo_results.delay_bound );
		assertEquals( "PMOO ARB backlog", new Num( 110 ), pmoo_results.backlog_bound );
	}

//--------------------Flow 1--------------------	
	@Test
	public void f1_tfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseFifoMultiplexing( true ); // default = false
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f1 );
		
		if ( tfa_results.failure == true ) {
			System.out.println( "TFA analysis failed" );
			System.out.println();
			
			if( !config.arrivalBoundMethods().contains( ArrivalBoundMethods.PMOO ) ) {
				assertEquals( "Unexpected exception occured", 0, 1 );
			}
			
			return;
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tTotal Flow Analysis (TFA)" );
			System.out.println( "Multiplexing:\t\tFIFO" );
	
			System.out.println( "Flow of interest:\t" + f1.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}

		assertEquals( "TFA FIFO delay", new Num( 13.5 ), tfa_results.delay_bound );
		assertEquals( "TFA FIFO backlog", new Num( 125 ), tfa_results.backlog_bound );
	}

	@Test
	public void f1_tfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true ); // s0.useArbitraryMultiplexing() != s0.useFifoMultiplexing()
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f1 );
		
		if ( tfa_results.failure == true ) {
			System.out.println( "TFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tTotal Flow Analysis (TFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f1.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}
		
		assertEquals( "TFA ARB delay", new Num( 135 ), tfa_results.delay_bound );
		assertEquals( "TFA ARB backlog", new Num( 125 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f1_sfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseFifoMultiplexing( true );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f1 );
		
		if ( sfa_results.failure == true ) {
			System.out.println( "SFA analysis failed" );
			System.out.println();
			
			if( !config.arrivalBoundMethods().contains( ArrivalBoundMethods.PMOO ) ) {
				assertEquals( "Unexpected exception occured", 0, 1 );
			}
			
			return;
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tSeparate Flow Analysis (SFA)" );
			System.out.println( "Multiplexing:\t\tFIFO" );
	
			System.out.println( "Flow of interest:\t" + f1.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}

		assertEquals( "SFA FIFO delay", new Num( 91, 6 ), sfa_results.delay_bound );
		assertEquals( "SFA FIFO backlog", new Num( 80 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f1_sfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f1 );
		
		if ( sfa_results.failure == true ) {
			System.out.println( "SFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tSeparate Flow Analysis (SFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f1.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}
		
		assertEquals( "SFA ARB delay", new Num( 22.5 ), sfa_results.delay_bound );
		assertEquals( "SFA ARB backlog", new Num( 350, 3 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f1_pmoo_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.SERVER_LOCAL );
		s0.setUseArbitraryMultiplexing( true );
		
		PmooAnalysisResults pmoo_results = Analysis.performPmooEnd2End( network, config, f1 );
		
		if ( pmoo_results.failure == true ) {
			System.out.println( "PMOO analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tPay Multiplexing Only Once (PMOO)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f1.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );

			System.out.println( "e2e PMOO SCs    : " + pmoo_results.betas_e2e );
			System.out.println( "xtx per server  : " + pmoo_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + pmoo_results.delay_bound );
			System.out.println( "backlog bound   : " + pmoo_results.backlog_bound );
		}

		assertEquals( "PMOO ARB delay", new Num( 22.5 ), pmoo_results.delay_bound );
		assertEquals( "PMOO ARB backlog", new Num( 350, 3 ), pmoo_results.backlog_bound );
	}
}