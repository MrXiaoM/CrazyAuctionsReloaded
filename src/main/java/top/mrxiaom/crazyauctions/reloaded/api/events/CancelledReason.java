package top.mrxiaom.crazyauctions.reloaded.api.events;

public enum CancelledReason
{
    /**
     * Cancelled by an administrator.
     */
    ADMIN_FORCE_CANCEL,
    /**
     * Cancelled by the player them self.
     */
    PLAYER_FORCE_CANCEL
}