import { useParams } from "react-router-dom";
export default function Team() {
    const params = useParams();
    return (
        <>
            <h3>
                [ Team Overview ]
                <br />
                Team ID: {params.teamId} |
                Group ID: {params.groupId}
            </h3>
        </>
    );
}