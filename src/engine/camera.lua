-- Declare constants
local CAMERA_SENSITIVITY = 500
local CAMERA_SPEED = 8

-- Declare varialbes
local UserInputService = nil
local RunService = nil
local camera = nil
local inputDirection = Vector2.new(0,0)

-- Initialize
UserInputService = game:GetService("UserInputService")
RunService = game:GetService("RunService")

--------------------------------------------------------
------------- Main Functionality Below -----------------
--------------------------------------------------------

RunService.RenderStepped:Connect(function(delta)
	-- Disable if game is running
	if ( game.Running ) then
		return
	end
	
	-- Disable if no camera
	camera = game.Workspace.CurrentCamera
	if ( camera == nil ) then
		return
	end
	
	-- Handle mouse grabbing
	local rightDown = UserInputService:IsMouseButtonDown(Enum.Mouse.Right)
	if ( rightDown ) then
		if ( not UserInputService.LockMouse ) then
			UserInputService.LockMouse = true
		end
		
		local mouseDelta = UserInputService:GetMouseDelta()
		
		-- Adjust Yaw and Pitch based on mouse delta
		camera.Yaw = camera.Yaw - mouseDelta.X/CAMERA_SENSITIVITY
		camera.Pitch = camera.Pitch - mouseDelta.Y/CAMERA_SENSITIVITY
	else
		if ( UserInputService.LockMouse ) then
			UserInputService.LockMouse = false
		end
	end
	
	-- Translate camera from keyboard input
	local cameraOffset = UserInputService:GetMovementVector(true)*delta*CAMERA_SPEED
	camera:Translate(cameraOffset)
end)
