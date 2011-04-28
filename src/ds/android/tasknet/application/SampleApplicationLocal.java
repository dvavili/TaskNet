/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ds.android.tasknet.application;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

import ds.android.tasknet.mfcc.MFCC;
import ds.android.tasknet.config.Preferences;
/* * @author Divya_PKV
 */
public class SampleApplicationLocal {

    public ArrayList<Double> method1(int a, int b) {
    	
    	// a should be less than b
    	if(a > b) {
    		int temp = a;
    		a = b;
    		b = temp;
    	}
    	
        int nnumberofFilters = 24;	
        int nlifteringCoefficient = b;	//earlier value was 22, now set to a-20
        boolean oisLifteringEnabled = true;
        boolean oisZeroThCepstralCoefficientCalculated = false;
        int nnumberOfMFCCParameters = a; //earlier value was 12, now set to a-10//without considering 0-th
        double dsamplingFrequency = 8000.0;
        int nFFTLength = 512;
        ArrayList<Double> mfcc_parameters = new ArrayList<Double>();
        
        if (oisZeroThCepstralCoefficientCalculated) {
          //take in account the zero-th MFCC
          nnumberOfMFCCParameters = nnumberOfMFCCParameters + 1;
        }
        else {
          nnumberOfMFCCParameters = nnumberOfMFCCParameters;
        }

        MFCC mfcc = new MFCC(nnumberOfMFCCParameters,
                             dsamplingFrequency,
                             nnumberofFilters,
                             nFFTLength,
                             oisLifteringEnabled,
                             nlifteringCoefficient,
                             oisZeroThCepstralCoefficientCalculated);
        //simulate a frame of speech
        double[] x = new double[160];
        Random rand = new Random();
        x[2]= rand.nextDouble(); x[4]= rand.nextDouble();
        double[] dparameters = mfcc.getParameters(x);
        for (int i = 0; i < dparameters.length; i++) 
        {
        	mfcc_parameters.add(dparameters[i]);
        }
        if(Preferences.DEBUG_MODE)
			System.out.println("DEBUG SET MFCC result: "+mfcc_parameters);	     	 
        
		return mfcc_parameters;
    }

    public void method2() {
        System.out.println("Method2");
    }

    public void method3() {
        System.out.println("Method3");
    }

    public void method4(int x, float y, String z, double d, int a, int b) {
        System.out.println("Method4");
    }
}
