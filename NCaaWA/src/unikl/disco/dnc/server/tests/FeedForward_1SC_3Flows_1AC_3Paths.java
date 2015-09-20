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
import java.util.LinkedList;
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
import unikl.disco.dnc.shared.network.Link;
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
public class FeedForward_1SC_3Flows_1AC_3Paths
{
	Configuration config;
	
	static final ServiceCurve service_curve = ServiceCurve.createRateLatency( 20, 20 );
	static final ArrivalCurve arrival_curve = ArrivalCurve.createTokenBucket( 5, 25 );
	
	static Network network;
	static Server s0, s1, s2, s3;
	static Flow f0, f1, f2;
	static Link l_s0_s1, l_s1_s3;
	
	@Parameters
	public static Collection<Object[]> data() {
		return FunctionalTests.createParameters();
	}
	 
	public FeedForward_1SC_3Flows_1AC_3Paths( HashSet<ArrivalBoundMethods> arrival_boundings, boolean iterative_ab, boolean remove_duplicates ) {
		config = FunctionalTests.printTestSettings( arrival_boundings, iterative_ab, remove_duplicates );
	}
	
	@BeforeClass
	static public void createNetwork()
	{
		network = new Network();
		s0 = network.addServer( service_curve );
		s1 = network.addServer( service_curve );
		s2 = network.addServer( service_curve );
		s3 = network.addServer( service_curve );

		try {
			l_s0_s1 = network.addLink( s0, s1 );
			network.addLink( s0, s3 );
			l_s1_s3 = network.addLink( s1, s3 );
			network.addLink( s2, s0 );
			network.addLink( s2, s1 );
			network.addLink( s2, s3 );
		} catch (Exception e) {
			System.out.println( e.toString() );
			assertEquals( "Unexpected exception occured", 0, 1 );
			return;
		}
		
		LinkedList<Link> f0_path = new LinkedList<Link>();
		f0_path.add( l_s0_s1 );
		f0_path.add( l_s1_s3 );

		try {	
			f0 = network.addFlow( arrival_curve, f0_path );
			f1 = network.addFlow( arrival_curve, s2, s3 );
			f2 = network.addFlow( arrival_curve, s2, s1 );
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
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
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
		
		assertEquals( "TFA FIFO delay", new Num( 5985, 64 ), tfa_results.delay_bound );
		assertEquals( "TFA FIFO backlog", new Num( 9425, 16 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f0_tfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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
		
		assertEquals( "TFA ARB delay", new Num( 6425, 36 ), tfa_results.delay_bound );
		assertEquals( "TFA ARB backlog", new Num( 6125, 9 ), tfa_results.backlog_bound );
	}

	@Test
	public void f0_sfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
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

		assertEquals( "SFA FIFO delay", new Num( 1795, 24 ), sfa_results.delay_bound );
		assertEquals( "SFA FIFO backlog", new Num( 3125, 8 ), sfa_results.backlog_bound );
	}

	@Test
	public void f0_sfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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
		
		assertEquals( "SFA ARB delay", new Num( 875, 9 ), sfa_results.delay_bound );
		assertEquals( "SFA ARB backlog", new Num( 4525, 9 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f0_pmoo_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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
		
		assertEquals( "PMOO ARB delay", new Num( 875, 9 ), pmoo_results.delay_bound );
		assertEquals( "PMOO ARB backlog", new Num( 4525, 9 ), pmoo_results.backlog_bound );
	}
	
//--------------------Flow 1--------------------	
	@Test
	public void f1_tfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
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
		
		assertEquals( "TFA FIFO delay", new Num( 3965, 64 ), tfa_results.delay_bound );
		assertEquals( "TFA FIFO backlog", new Num( 9425, 16 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f1_tfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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
		
		assertEquals( "TFA ARB delay", new Num( 11975, 90 ), tfa_results.delay_bound );
		assertEquals( "TFA ARB backlog", new Num( 6125, 9 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f1_sfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
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

		assertEquals( "SFA FIFO delay", new Num( 2675, 48 ), sfa_results.delay_bound );
		assertEquals( "SFA FIFO backlog", new Num( 4725, 16 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f1_sfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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

		assertEquals( "SFA ARB delay", new Num( 230, 3 ), sfa_results.delay_bound );
		assertEquals( "SFA ARB backlog", new Num( 400 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f1_pmoo_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
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

		assertEquals( "PMOO ARB delay", new Num( 230,03  ), pmoo_results.delay_bound );
		assertEquals( "PMOO ARB backlog", new Num( 400 ), pmoo_results.backlog_bound );
	}
	
//--------------------Flow 2--------------------	
	@Test
	public void f2_tfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f2 );
		
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
	
			System.out.println( "Flow of interest:\t" + f2.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}
		
		assertEquals( "TFA FIFO delay", new Num( 885, 16 ), tfa_results.delay_bound );
		assertEquals( "TFA FIFO backlog", new Num( 1825, 4 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f2_tfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
		TotalFlowAnalysisResults tfa_results = Analysis.performTfaEnd2End( network, config, f2 );
		
		if ( tfa_results.failure == true ) {
			System.out.println( "TFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tTotal Flow Analysis (TFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f2.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
					
			System.out.println( "delay bound     : " + tfa_results.delay_bound );
			System.out.println( "     per server : " + tfa_results.map__server__D_server.toString() );
			System.out.println( "backlog bound   : " + tfa_results.backlog_bound );
			System.out.println( "     per server : " + tfa_results.map__server__B_server.toString() );
			System.out.println( "alpha per server: " + tfa_results.map__server__alphas.toString() );
		}
		
		assertEquals( "TFA ARB delay", new Num( 685, 6 ), tfa_results.delay_bound );
		assertEquals( "TFA ARB backlog", new Num( 1475, 3 ), tfa_results.backlog_bound );
	}
	
	@Test
	public void f2_sfa_fifoMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_FIFO );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f2 );
		
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
	
			System.out.println( "Flow of interest:\t" + f2.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}

		assertEquals( "SFA FIFO delay", new Num( 295, 6 ), sfa_results.delay_bound );
		assertEquals( "SFA FIFO backlog", new Num( 525, 2 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f2_sfa_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
		SeparateFlowAnalysisResults sfa_results = Analysis.performSfaEnd2End( network, config, f2 );
		
		if ( sfa_results.failure == true ) {
			System.out.println( "SFA analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tSeparate Flow Analysis (SFA)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f2.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );
			
			System.out.println( "e2e SFA SCs     : " + sfa_results.betas_e2e );
			System.out.println( "     per server : " + sfa_results.map__server__betas_lo.toString() );
			System.out.println( "xtx per server  : " + sfa_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + sfa_results.delay_bound );
			System.out.println( "backlog bound   : " + sfa_results.backlog_bound );
		}

		assertEquals( "SFA ARB delay", new Num( 65 ), sfa_results.delay_bound );
		assertEquals( "SFA ARB backlog", new Num( 1025, 3 ), sfa_results.backlog_bound );
	}
	
	@Test
	public void f2_pmoo_arbMux()
	{
		config.setMultiplexingDiscipline( MuxDiscipline.GLOBAL_ARBITRARY );
		
		PmooAnalysisResults pmoo_results = Analysis.performPmooEnd2End( network, config, f2 );
		
		if ( pmoo_results.failure == true ) {
			System.out.println( "PMOO analysis failed" );
			System.out.println();
			
			assertEquals( "Unexpected exception occured", 0, 1 );
		}
	
		if( FunctionalTests.fullConsoleOutput() ) {
			System.out.println( "Analysis:\t\tPay Multiplexing Only Once (PMOO)" );
			System.out.println( "Multiplexing:\t\tArbitrary" );
	
			System.out.println( "Flow of interest:\t" + f2.toString() );
			System.out.println();
			
			System.out.println( "--- Results: ---" );

			System.out.println( "e2e PMOO SCs    : " + pmoo_results.betas_e2e );
			System.out.println( "xtx per server  : " + pmoo_results.map__server__alphas.toString() );
			System.out.println( "delay bound     : " + pmoo_results.delay_bound );
			System.out.println( "backlog bound   : " + pmoo_results.backlog_bound );
		}
		
		assertEquals( "PMOO ARB delay", new Num( 65 ), pmoo_results.delay_bound );
		assertEquals( "PMOO ARB backlog", new Num( 1025, 3 ), pmoo_results.backlog_bound );
	}
}