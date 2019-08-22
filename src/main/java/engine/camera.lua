-- Declare constants
local CAMERA_SENSITIVITY = 500
local CAMERA_SPEED = 10
local EPSILON = 1/64

-- Declare varialbes
local UserInputService = nil
local RunService = nil
local inputDirection = Vector2.new(0,0)

-- Initialize
UserInputService = game:GetService("UserInputService")
RunService = game:GetService("RunService")

--------------------------------------------------------
------------- Main Functionality Below -----------------
--------------------------------------------------------

UserInputService.InputBegan:Connect(function(InputData)
	-- Disable if no camera
	local camera = game.Workspace.CurrentCamera
	if ( camera == nil ) then
		return
	end
	
	-- Disable if camera is not in freecam
	if ( camera.CameraType ~= Enum.CameraType.Free ) then
		return
	end

	-- Get the look vector
	local LookVector = camera:GetLookVector()

	-- Scroll zooming
	if ( InputData.UserInputType == Enum.UserInputType.Mouse ) then
		if ( InputData.Button == Enum.Mouse.WheelUp ) then
			camera:Translate(LookVector)
		elseif ( InputData.Button == Enum.Mouse.WheelDown ) then
			camera:Translate(LookVector*-1)
		end
	end
end)

RunService.RenderStepped:Connect(function(delta)	
	-- Disable if no camera
	local camera = game.Workspace.CurrentCamera
	if ( camera == nil ) then
		return
	end
	
	-- Disable if camera is not in freecam
	if ( camera.CameraType ~= Enum.CameraType.Free ) then
		return
	end
	
	-- Get Camera speed
	local CameraSpeed = CAMERA_SPEED * (UserInputService:IsKeyDown(Enum.KeyCode.LeftShift) and 0.25 or 1)
	
	-- Vertical UP moving
	if ( UserInputService:IsKeyDown(Enum.KeyCode.E) ) then
		camera:Translate(Vector3.new(0, 0, 1)*delta*CameraSpeed*0.5)
	end
	
	-- Vertical DOWN moving
	if ( UserInputService:IsKeyDown(Enum.KeyCode.Q) ) then
		camera:Translate(Vector3.new(0, 0, -1)*delta*CameraSpeed*0.5)
	end
	
	-- Handle mouse grabbing
	if ( UserInputService:IsMouseButtonDown(Enum.Mouse.Right) ) then
		if ( not UserInputService.LockMouse ) then
			UserInputService.LockMouse = true
		end
		
		local mouseDelta = UserInputService:GetMouseDelta()
		
		-- Adjust Yaw and Pitch based on mouse delta
		camera.Yaw = camera.Yaw - mouseDelta.X/CAMERA_SENSITIVITY
		camera.Pitch = math.min( math.pi/2-EPSILON, math.max( -math.pi/2+EPSILON, camera.Pitch - mouseDelta.Y/CAMERA_SENSITIVITY ) )
	else
		if ( UserInputService.LockMouse ) then
			UserInputService.LockMouse = false
		end
	end
	
	-- Translate camera from keyboard input
	local cameraOffset = UserInputService:GetMovementVector(true)*delta*CameraSpeed
	camera:Translate(cameraOffset)
end)
