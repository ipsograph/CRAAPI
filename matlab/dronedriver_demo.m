%% Setup
bdclose all;
clear all;
bdclose all;
close all;
wsroot = pwd; % change as needed
setJavaClasspath(wsroot, 1, 1);

%% Load filters (and reorganize)
load('./data/action_filters.mat');
n_filters = size(action_filters,4);
filters = cell(n_filters);
for i=1:n_filters
    filters{i} = action_filters(:,:,:,i);
end
gauss = fspecial('gaussian',[7 7], 3);
filter_labels = {'stationary','left','up','right','down'};
roll_vals = {'roll=0.0', 'roll=-0.1', 'roll=0.0', 'roll=0.1', 'roll=0.0'};
alt_vals = {'alt=0.0', 'alt=0.0', 'alt=0.1', 'alt=0.0', 'alt=-0.1'};
samplesize = 9;

%% Set flags for this run (TODO user input)
simflag = 1;
testflag = 0;
vidflag = 1;
drawflag = 1;
gaussflag = 1;
opponentflag = 1;
takeoffflag = 0;
fpsCap = 10;
maxRetainedFrames = 10;
maxIterations = 600; % -1 for infinite
fwidth = 640;
fheight = 360;
opponent_const = sqrt(2);

%% Run...
if testflag == 1
    
    % run some static/canned tests
    org.craapi.drone.matlab.MatlabDrone.mainForMatlab(simflag);
    if vidflag == 1
        org.craapi.drone.matlab.VideoCapture.mainForMatlab();
    end
    
else
    
    % Create a drone
    drone = createJavaDrone(simflag);
    if vidflag == 1
        video = createJavaVideo();
        video.startImageCapture(fpsCap, maxRetainedFrames);
    end

    % Initialize and connect to the drone
    drone.init();
    drone.startDrone();
    
    % Takeoff
    if takeoffflag == 1
        drone.takeoff();
    end
    
    % Do some drone stuff...
    iter = 0;
    running = 1;
    frame = zeros(fheight, fwidth);
    frames_tiny = zeros(fheight/8, fwidth/8, samplesize);
    
    % display
    imhandles = cell(3,1);
    plothandles = cell(3,1);
    
    % figure for streaming video
    h  = figure('ToolBar','none');
    set(gcf,'DoubleBuffer','on');
    imhandles{1} = imshow(zeros(fheight, fwidth)); set(imhandles{1},'erasemode','xor');
    plothandles{1} = title('Drone Video (680x360)');
    
    % create a button to turn off drone (hopefully)
    ht = uitoolbar(h);
    [xico, icom] = imread(fullfile(wsroot,'data','warning.gif'));
    icon = ind2rgb(xico, icom);
    hpt = uipushtool(ht,'CData',icon, 'TooltipString','Stop Drone', 'UserData','go', 'ClickedCallback','running=0;');
    
    % figure for indexed images/results
    figure(2);
    set(gcf,'DoubleBuffer','on')
    cmap = jet(5);
    subplot(2,1,1);
    imhandles{2} = imshow(zeros(fheight/8-8, fwidth/8-8), 'Colormap', cmap); set(imhandles{2},'erasemode','xor');
    plothandles{2} = title('Indexed Responses');
    caxis([0 4]);
    
    subplot(2,1,2);
    imhandles{3} = imshow(zeros(fheight/8-8, fwidth/8-8), 'Colormap', cmap); set(imhandles{3},'erasemode','xor');
    plothandles{3} = title('Pooled Responses (stationary)');
    caxis([0 4]);
    
    fcount = 0;
    while running == 1 && (maxIterations < 0 || iter < maxIterations) && ((drone.isAirborne() == 1) || (takeoffflag == 0))
        
        % get a single frame (wait up to 200 milliseconds)
        buff = video.getFrame(200);
        [h,w,cs] = getFrameStats(buff);
        
        % valid frame?
        if h > 0
            f_idx = mod(fcount,samplesize)+1;
            frame = droneFrameToImage(droneFrameToGray(buff),h,w);
            
            frames_tiny(:,:,2:samplesize) = frames_tiny(:,:,1:samplesize-1);
            frames_tiny(:,:,1) = imresize(frame,[fheight/8, fwidth/8]);
            
            if gaussflag == 1
                frames_tiny(:,:,1) = imfilter(frames_tiny(:,:,1),gauss,'same');
            end
            
            fcount = fcount + 1;
            if fcount >= samplesize
                
                % got x frames to work with
                fresult_tiny = filterFrameSequence(frames_tiny,filters);
                
                if opponentflag == 1
                    fresult_tiny = opponentNorm(fresult_tiny,[3, 4, 1, 2],opponent_const);
                end
                
                [mresult_tiny, midx_tiny] = maxResults(fresult_tiny, 0.02);
                
                %[histGrid,histwin] = gridResults(fresult_tiny,mresult_tiny,midx_tiny,5,5,0.3,'hist');
                %[maxGrid,maxwin] = gridResults(fresult_tiny,mresult_tiny,midx_tiny,5,5,0.1,'max');
                [sumGrid,sumwin] = gridResults(fresult_tiny,mresult_tiny,midx_tiny,5,5,0.6,'sum');
                
                if drawflag == 1
                    set(imhandles{1},'cdata',frame);
                    set(imhandles{2},'cdata',(midx_tiny(:,:)));
                    set(imhandles{3},'cdata',(sumGrid(:,:)));
                    set(plothandles{3},'String',sprintf('Pooled Responses (%s)',filter_labels{sumwin}));
                    drawnow;
                end
                
                if takeoffflag == 1
                    
                    % make small adjustment
                    ypr = strcat(roll_vals{sumwin},',',alt_vals{sumwin});
                    drone.setKinematics(ypr);
                    
                    % wait 1 second
                    pause('on');
                    pause(1);
                    
                    % hover
                    drone.setKinematics('hover=true');
                    
                    % wait 1 second
                    pause(1);
                    
                    % clear buffer (will not be valid after pausing/moving)
                    fcount = 0;
                    
                end
            end
        end
        
        % keep track of number of loops (bail after maxIterations)
        iter = iter + 1;
    end
    
    % Close (if not already closed)
    video.stopImageCapture();
    drone.stopDrone();
    
end
